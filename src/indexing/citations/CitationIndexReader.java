package indexing.citations;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.FileReaderWriterFactory;
import io.index.IndexReader;

public class CitationIndexReader implements AutoCloseable {
	
	/**
	 * Contains the file reader for the index file.
	 */
	private IndexReader indexFile;
	
	/**
	 * Contains the corresponding seek list.
	 */
	private CitationIndexSeekList seekList;
	
	/**
	 * Contains the number of all citations in the index.
	 */
	private int totalCitationsCount = 0;
	
	
	/**
	 * Creates a new InvertedIndexReader instance.
	 * @param indexFile
	 * @param seekListFile
	 * @param isCompressed
	 * @throws IOException
	 */
	public CitationIndexReader(File indexFile, File seekListFile, boolean isCompressed) throws IOException {
		this.indexFile = FileReaderWriterFactory.getInstance().getDirectIndexReader(indexFile, isCompressed);
		this.totalCitationsCount = this.indexFile.readInt();
		
		this.seekList = new CitationIndexSeekList();
		this.seekList.load(FileReaderWriterFactory.getInstance().getDirectIndexReader(seekListFile, isCompressed));
	}
	
	
	/**
	 * Gets the number of all tokens occurrences in the index.
	 * @return
	 */
	public int getTotalTokenCount() {
		return this.totalCitationsCount;
	}
	
	
	/**
	 * Gets ids of documents that cite the given one.
	 * @param documentId
	 * @return
	 * @throws IOException
	 */
	public Set<Integer> getLinkingDocuments(int documentId) throws IOException {
		long offset = this.seekList.get(documentId);
		return this.getLinkingDocuments(documentId, offset);
	}
	
	/**
	 * Gets ids of documents that cite the given one.
	 * @param documentId
	 * @param offset
	 * @return
	 * @throws IOException
	 */
	private Set<Integer> getLinkingDocuments(int documentId, long offset) throws IOException {
		this.indexFile.seek(offset);
		while(true) {
			try {
				int readDocumentId = this.indexFile.readInt();
				if(readDocumentId == documentId) {
					this.indexFile.getSkippingAreaLength();
					return this.readDocumentIds();
				}
				
				this.indexFile.skipSkippingArea();
			}
			catch(EOFException e) {
				break;
			}
		}
		
		return new HashSet<Integer>();
	}
	
	/**
	 * Reads the list of document ids from index file.
	 * @return
	 * @throws IOException
	 */
	private Set<Integer> readDocumentIds() throws IOException {
		int count = this.indexFile.readInt();
		Set<Integer> documentIds = new HashSet<Integer>(count);
		int lastDocumentId = 0;
		for(int i = 0; i < count; i++) {
			int documentId = this.indexFile.readInt();
			if(this.indexFile.isCompressed()) {
				documentId += lastDocumentId;
				lastDocumentId = documentId;
			}
			documentIds.add(documentId);
		}
		
		return documentIds;
	}

	
	@Override
	public void close() throws Exception {
		this.indexFile.close();
	}	
}
