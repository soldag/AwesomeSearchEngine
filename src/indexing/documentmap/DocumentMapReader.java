package indexing.documentmap;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import documents.PatentDocument;

public class DocumentMapReader implements AutoCloseable {

	/**
	 * Contains the file reader for the document map.
	 */
	private RandomAccessFile documentMapFile;
	
	/**
	 * Contains the number of documents contained in the map.
	 */
	private int documentsCount = 0;
	
	
	/**
	 * Creates a new DocumentMapReader instance.
	 * @param documentMapFile
	 * @throws IOException
	 */
	public DocumentMapReader(File documentMapFile) throws IOException {
		this.documentMapFile = new RandomAccessFile(documentMapFile, "r");
		this.documentsCount = this.documentMapFile.readInt();
	}
	
	
	/**
	 * Gets the number of documents contained in the map.
	 * @return
	 */
	public int getDocumentsCount() {
		return this.documentsCount;
	}
	
	/**
	 * Gets a document from map by specifying its id and a start offset in the map file.
	 * @param documentId
	 * @param startOffset
	 * @return PatentDocument
	 * @throws IOException
	 */
	public PatentDocument getDocument(int documentId, int startOffset) throws IOException {
		this.documentMapFile.seek(startOffset);
		while(true) {
			try {
				// Read document id
				int readDocumentId = this.documentMapFile.readInt();
				
				// Read length of properties
				int propertiesLength = this.documentMapFile.readInt();
				
				if(readDocumentId == documentId) {
					byte[] propertyBytes = new byte[propertiesLength];
					this.documentMapFile.readFully(propertyBytes);
					
					return PatentDocument.fromPropertyBytes(readDocumentId, propertyBytes);
				}
				else {
					this.documentMapFile.seek(this.documentMapFile.getFilePointer() + propertiesLength);
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
