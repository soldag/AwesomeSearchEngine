package indexing.documentmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import parsing.PatentDocument;

public class DocumentMapConstructor implements AutoCloseable {

	/**
	 * Contains the corresponding seek list, if it should be constructed.
	 */
	private DocumentMapSeekList seekList;
	
	/**
	 * Contains the file write for the document map.
	 */
	private RandomAccessFile documentMapWriter;	
	
	/**
	 * Contains the number of documents contained in this map.
	 */
	private int documentsCount = 0;
	
	
	/**
	 * Creates a new DocumentMapConstructor instance, that does not create a seek list.
	 * @param documentMapFile
	 * @throws IOException
	 */
	public DocumentMapConstructor(File documentMapFile) throws IOException {
		this(documentMapFile, null);
	}
	
	/**
	 * Creates a new DocumentMapConstructor instance, that creates a seek list.
	 * @param documentMapFile
	 * @param seekList
	 * @throws IOException
	 */
	public DocumentMapConstructor(File documentMapFile, DocumentMapSeekList seekList) throws IOException {
		this.seekList = seekList;
		this.documentMapWriter = new RandomAccessFile(documentMapFile, "rw");
		
		// Initialize documents count in index
		this.documentMapWriter.writeInt(0);
	}
	
	
	/**
	 * Adds a document to the map.
	 * @param document
	 * @throws IOException
	 */
	public void add(PatentDocument document) throws IOException {	
		// Add document to seek list
		this.seekList.put(document.getId(), (int)this.documentMapWriter.getFilePointer());
		
		// Write to file
		byte[] patentDocumentBytes = document.toBytes();
		this.documentMapWriter.write(patentDocumentBytes);
		
		// Increase total number of documents
		this.documentsCount++;		
	}
	
	/**
	 * Writes the constructed seek list to file.
	 * @param seekListFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void writeSeekList(File seekListFile) throws FileNotFoundException, IOException {
		try(RandomAccessFile seekListWriter = new RandomAccessFile(seekListFile, "rw")) {
			this.seekList.save(seekListWriter);
		}
	}
	
	/**
	 * Updates the total number of documents and closes this resource, relinquishing any underlying resources.
	 */
	public void close() throws IOException {
		// Write number of documents
		this.documentMapWriter.seek(0);
		this.documentMapWriter.writeInt(this.documentsCount);
		
		this.documentMapWriter.close();
	}
}
