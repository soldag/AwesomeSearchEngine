package indexing.documentmap;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import documents.PatentDocument;
import io.FileReaderWriterFactory;
import io.index.IndexReader;

public class DocumentMapReader implements AutoCloseable {

	/**
	 * Contains the file reader for the document map.
	 */
	private IndexReader documentMapFile;
	
	/**
	 * Contains the corresponding seek list.
	 */
	private DocumentMapSeekList seekList;
	
	/**
	 * Contains the number of all documents stored in the index.
	 */
	private int totalDocumentsCount = 0;
	
	
	/**
	 * Creates a new DocumentMapReader instance.
	 * @param documentMapFile
	 * @param documentMapSeekListFile
	 * @throws IOException
	 */
	public DocumentMapReader(File documentMapFile, File documentMapSeekListFile, boolean isCompressed) throws IOException {
		this.documentMapFile = FileReaderWriterFactory.getInstance().getMemoryMappedIndexReader(documentMapFile, isCompressed);
		this.totalDocumentsCount = this.documentMapFile.readInt();
		
		this.seekList = new DocumentMapSeekList();
		this.seekList.load(FileReaderWriterFactory.getInstance().getDirectIndexReader(documentMapSeekListFile, isCompressed));
	}
	
	
	/**
	 * Gets the number of all documents stored in the index.
	 * @return
	 */
	public int getTotalDocumentsCount() {
		return this.totalDocumentsCount;
	}	
	
	/**
	 * Gets a document from map by specifying its id
	 * @param documentId
	 * @return PatentDocument
	 * @throws IOException
	 */
	public PatentDocument getDocument(int documentId) throws IOException {
		long offset = this.seekList.get(documentId);
		if(offset > 0) {
			return this.getDocument(documentId, offset);
		}
		
		return null;
	}
	
	/**
	 * Gets a document from map by specifying its id and a start offset in the map file.
	 * @param documentId
	 * @param startOffset
	 * @return PatentDocument
	 * @throws IOException
	 */
	private PatentDocument getDocument(int documentId, long startOffset) throws IOException {
		this.documentMapFile.seek(startOffset);
		while(true) {
			try {
				// Read document id
				int readDocumentId = this.documentMapFile.readInt();
				
				// Read skip pointer
				int skipPointer = this.documentMapFile.readInt();
				
				if(readDocumentId == documentId) {
					return PatentDocument.load(readDocumentId, documentMapFile);
				}
				else {
					this.documentMapFile.seek(this.documentMapFile.getFilePointer() + skipPointer);
				}
			}
			catch(EOFException e) {
				return null;
			}
		}
	}
	
	/**
	 * Closes this resource, relinquishing any underlying resources.
	 */
	public void close() throws IOException {
		this.documentMapFile.close();
	}
}
