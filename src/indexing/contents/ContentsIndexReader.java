package indexing.contents;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;

import indexing.documentmap.DocumentMapSeekList;
import io.FileReaderWriterFactory;
import io.index.IndexReader;
import postings.ContentType;

public class ContentsIndexReader implements AutoCloseable {

	/**
	 * Contains the file reader for the contents index.
	 */
	private IndexReader contentsIndexFile;
	
	/**
	 * Contains the corresponding seek list.
	 */
	private DocumentMapSeekList seekList;
	
	/**
	 * Contains the number of all documents stored in the index.
	 */
	private int totalDocumentsCount = 0;
	
	
	/**
	 * Creates a new ContentsIndexReader instance.
	 * @param contentsIndexFile
	 * @param contentsIndexSeekListFile
	 * @param isCompressed
	 * @throws IOException
	 */
	public ContentsIndexReader(File contentsIndexFile, File contentsIndexSeekListFile, boolean isCompressed) throws IOException {
		this.contentsIndexFile = FileReaderWriterFactory.getInstance().getMemoryMappedIndexReader(contentsIndexFile, isCompressed);
		this.totalDocumentsCount = this.contentsIndexFile.readInt();
		
		this.seekList = new DocumentMapSeekList();
		this.seekList.load(FileReaderWriterFactory.getInstance().getDirectIndexReader(contentsIndexSeekListFile, isCompressed));
	}
	
	
	/**
	 * Gets the number of all documents stored in the index.
	 * @return
	 */
	public int getTotalDocumentsCount() {
		return this.totalDocumentsCount;
	}
	
	/**
	 * Gets the contents of a document from index by specifying its id.
	 * @param documentId
	 * @return
	 * @throws IOException
	 */
	public EnumMap<ContentType, String> getContents(int documentId) throws IOException {
		long startOffset = this.seekList.get(documentId);
		if(startOffset > 0) {
			return this.getContents(documentId, startOffset);
		}
		
		return null;
	}
	
	/**
	 * Gets the contents of a document from index by specifying its id and a start offset in the index file.
	 * @param documentId
	 * @param startOffset
	 * @return
	 * @throws IOException
	 */
	private EnumMap<ContentType, String> getContents(int documentId, long startOffset) throws IOException {
		this.contentsIndexFile.seek(startOffset);
		while(true) {
			try {
				// Read document id
				int readDocumentId = this.contentsIndexFile.readInt();
				
				if(readDocumentId == documentId) {
					this.contentsIndexFile.getSkippingAreaLength();
					return this.readContents();
				}
				
				this.contentsIndexFile.skipSkippingArea();
			}
			catch(EOFException e) {
				return null;
			}
		}
	}
	
	/**
	 * Reads the contents from the current position in the contents index file.
	 * @return
	 * @throws IOException
	 */
	private EnumMap<ContentType, String> readContents() throws IOException {
		EnumMap<ContentType, String> contents = new EnumMap<ContentType, String>(ContentType.class);
		for(ContentType contentType: ContentType.orderedValues()) {
			String content = this.contentsIndexFile.readString();
			if(content != null && !content.isEmpty()) {
				contents.put(contentType, content);
			}
		}
		
		return contents;
	}
	
	
	/**
	 * Closes this resource, relinquishing any underlying resources.
	 */
	public void close() throws IOException {
		this.contentsIndexFile.close();
	}
}
