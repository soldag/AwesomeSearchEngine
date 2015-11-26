package parsing.lookups;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import parsing.PatentDocument;

public abstract class AbstractPatentPropertyLookup {
	
	/**
	 * Contains the path of the data directory, containing the document files.
	 */
	private final Path documentDirectory;
	
	
	/**
	 * Creates a new AbstractPatentPropertyLookup instance.
	 * @param documentDirectory
	 */
	public AbstractPatentPropertyLookup(Path documentDirectory) {
		this.documentDirectory = documentDirectory;
	}
	
	
	/**
	 * Gets a specific property from the given patent document.
	 * @param document
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public abstract String get(PatentDocument document) throws FileNotFoundException, IOException;
	
	/**
	 * Reads a text section of a document file (specified by its file id) determined by its byte offset and length. 
	 * XML tags are removed during extraction.
	 * @param fileId
	 * @param offset
	 * @param length
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected String readFromFile(int fileId, long offset, int length) throws FileNotFoundException, IOException {
		File sourceFile = documentDirectory.resolve(this.getFileName(fileId)).toFile();
		try(RandomAccessFile file = new RandomAccessFile(sourceFile, "r")) {
			file.seek(offset);
			byte[] buffer = new byte[length];
			file.readFully(buffer);
			String title = new String(buffer);
			
			return this.removeXML(title);
		}
	}

	/**
	 * Constructs the file name of a document file by specifying its file id.
	 * @param fileId
	 * @return
	 */
	private String getFileName(int fileId) {
		return String.format("ipg%s.xml", fileId);
	}
	
	/**
	 * Removes all xml tags from a given string.
	 * @param text
	 * @return
	 */
	private String removeXML(String text) {
		return text.replaceAll("<[^>]+>?","").trim();
	}
}
