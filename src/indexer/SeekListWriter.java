package indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class SeekListWriter implements AutoCloseable {
	
	public static final String SEPARATOR = "|";
	
	/**
	 * Determines, how many often a token should be written to seek list (every n-th token).
	 */
	public static final int SEEK_LIST_TOKEN_LIMIT = 200;
	
	private BufferedWriter fileWriter;
	
	private int tokenCount = 0;
	
	public SeekListWriter(File seekListFile) throws UnsupportedEncodingException, FileNotFoundException {
		this.fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(seekListFile), "UTF-8"));
	}

	public void write(String token, long offset) throws IOException {
		// Reset number of tokens, if limit has been reached
		if(this.tokenCount == SEEK_LIST_TOKEN_LIMIT) {
			this.tokenCount = 0;
		}
		if(this.tokenCount == 0) {
			// Write seek list entry for the current token
			this.fileWriter.write(token + SEPARATOR + offset);
			this.fileWriter.newLine();
		}
		this.tokenCount++;
	}
	
	public void close() throws IOException {
		this.fileWriter.close();
	}
}
