package indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class DocumentMapWriter implements AutoCloseable {
	
	public static final String SEPARATOR = "|";
	
	private BufferedWriter fileWriter;
	
	public DocumentMapWriter(File documentMapFile) throws UnsupportedEncodingException, FileNotFoundException {
		this.fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(documentMapFile), "UTF-8"));
	}

	public void write(Integer documentId, String documentPath) throws IOException {
		this.fileWriter.write(documentId + SEPARATOR + documentPath);
		this.fileWriter.newLine();
	}
	
	public void close() throws IOException {
		this.fileWriter.close();
	}
}
