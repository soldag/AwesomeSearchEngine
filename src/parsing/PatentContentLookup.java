package parsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import org.apache.commons.lang3.tuple.Pair;

import documents.PatentDocument;
import postings.ContentType;

public class PatentContentLookup {
	
	/**
	 * Contains the path of the data directory, containing the document files.
	 */
	private final Path documentDirectory;
	
	
	/**
	 * Creates a new PatentContentLookup instance.
	 * @param documentDirectory
	 */
	public PatentContentLookup(Path documentDirectory) {
		this.documentDirectory = documentDirectory;
	}
	
	
	/**
	 * Gets a specific content type from the given patent document.
	 * @param document
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String get(PatentDocument document, ContentType contentType) throws FileNotFoundException, IOException {
		if(document.hasContent(contentType)) {
			return this.readFromFile(document.getFileId(), document.getContentOffset(contentType));
		}
		
		return null;
	}
	
	/**
	 * Reads a text section of a document file (specified by its file id) determined by its position (byte offset and length). 
	 * XML tags are removed during extraction.
	 * @param fileId
	 * @param position
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected String readFromFile(int fileId, Pair<Long, Integer> position) throws FileNotFoundException, IOException {
		File sourceFile = this.documentDirectory.resolve(this.getFileName(fileId)).toFile();
		try(RandomAccessFile file = new RandomAccessFile(sourceFile, "r")) {
			file.seek(position.getLeft());
			byte[] buffer = new byte[position.getRight()];
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
		return String.format("ipg%0" + PatentDocument.FILE_ID_LENGTH + "d.xml", fileId);
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
