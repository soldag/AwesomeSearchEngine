package parsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import com.ximpleware.ParseException;

import documents.PatentContentDocument;
import documents.PatentDocument;
import io.FileReaderWriterFactory;
import io.lowlevel.FileReader;

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
	 * Loads the content of the given document.
	 * @param document
	 * @return
	 * @throws IOException
	 */
	public PatentContentDocument loadContent(PatentDocument document) throws IOException {
		// Read XML element of the given document from file
		byte[] documentBytes = this.readFromFile(document);
		
		// Parse element to PatentContentDocument
		try {
			PatentDocumentParser parser = new PatentDocumentParser(document.getFileId(), documentBytes);
			if(parser.hasNext()) {
				return parser.next();
			}
		} catch (ParseException e) { }
		
		return null;
	}
	
	/**
	 * Reads the patent XML element of the given document from corresponding document file.
	 * @param fileId
	 * @param position
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private byte[] readFromFile(PatentDocument document) throws IOException {
		File sourceFile = this.documentDirectory.resolve(this.getFileName(document.getFileId())).toFile();
		try(FileReader reader = FileReaderWriterFactory.getInstance().getDirectFileReader(sourceFile)) {
			reader.seek(document.getOffset());
			byte[] buffer = new byte[document.getLength()];
			reader.read(buffer);
			
			return buffer;
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
}
