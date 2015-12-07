package indexing.documentmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import documents.PatentDocument;
import io.FileFactory;
import io.FileWriter;

public class DocumentMapConstructor implements AutoCloseable {

	/**
	 * Contains the corresponding seek list, if it should be constructed.
	 */
	private DocumentMapSeekList seekList;
	
	/**
	 * Contains the file write for the document map.
	 */
	private FileWriter documentMapWriter;	

	/**
	 * Determines, whether the document map should be compressed.
	 */
	private boolean compress;
	
	
	/**
	 * Creates a new DocumentMapConstructor instance, that does not create a seek list.
	 * @param documentMapFile
	 * @throws IOException
	 */
	public DocumentMapConstructor(File documentMapFile, boolean compress) throws IOException {
		this(documentMapFile, null, compress);
	}
	
	/**
	 * Creates a new DocumentMapConstructor instance, that creates a seek list.
	 * @param documentMapFile
	 * @param seekList
	 * @throws IOException
	 */
	public DocumentMapConstructor(File documentMapFile, DocumentMapSeekList seekList, boolean compress) throws IOException {
		this.seekList = seekList;
		this.compress = compress;
		this.documentMapWriter = FileFactory.getInstance().getWriter(documentMapFile, this.compress);
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
		document.save(documentMapWriter);
	}
	
	/**
	 * Writes the constructed seek list to file.
	 * @param seekListFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void writeSeekList(File seekListFile) throws FileNotFoundException, IOException {
		FileWriter seekListWriter = FileFactory.getInstance().getWriter(seekListFile, this.compress);
		this.seekList.save(seekListWriter);
		seekListWriter.close();
	}
	
	/**
	 * Closes this resource, relinquishing any underlying resources.
	 */
	public void close() throws IOException {		
		this.documentMapWriter.close();
	}
}
