package indexing.documentmap;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import documents.PatentDocument;
import io.FileFactory;
import io.FileReader;

public class DocumentMapReader implements AutoCloseable {

	/**
	 * Contains the file reader for the document map.
	 */
	private FileReader documentMapFile;
	
	/**
	 * Contains the number of all documents stored in the index.
	 */
	private int totalDocumentsCount = 0;
	
	
	/**
	 * Creates a new DocumentMapReader instance.
	 * @param documentMapFile
	 * @throws IOException
	 */
	public DocumentMapReader(File documentMapFile, boolean isCompressed) throws IOException {
		this.documentMapFile = FileFactory.getInstance().getReader(documentMapFile, isCompressed);
		this.totalDocumentsCount = this.documentMapFile.readInt();
	}
	
	
	/**
	 * Gets the number of all documents stored in the index.
	 * @return
	 */
	public int getTotalDocumentsCount() {
		return this.totalDocumentsCount;
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
