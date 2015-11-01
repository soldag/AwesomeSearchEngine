package indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

public class SeekListWriter implements AutoCloseable {
	
	public static final String SEPARATOR = "|";
	
	/**
	 * Determines, how many often a token should be written to seek list (every n-th token).
	 */
	public static final int SEEK_LIST_TOKEN_LIMIT = 200;
	
	private RandomAccessFile fileWriter;
	private BufferedWriter secondarySeekListWriter;
	
	private int tokenCount = 0;
	
	public SeekListWriter(File seekListFile, File secondarySeekListFile) throws UnsupportedEncodingException, FileNotFoundException {
		this.fileWriter = new RandomAccessFile(seekListFile, "rw");
		this.secondarySeekListWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(secondarySeekListFile), "UTF-8"));
	}

	public void write(String token, long offset) throws IOException {
		// Reset number of tokens, if limit has been reached
		if(this.tokenCount == SEEK_LIST_TOKEN_LIMIT) {
			this.tokenCount = 0;
		}
		if(this.tokenCount == 0) {
			// Write seek list entry for the current token
			this.secondarySeekListWriter.write(token + SEPARATOR + this.fileWriter.getFilePointer());
			this.secondarySeekListWriter.newLine();
		}
		this.tokenCount++;		
		
		String line = token + SEPARATOR + offset + System.getProperty("line.separator");
		this.fileWriter.write(line.getBytes());
	}
	
	public void close() throws IOException {
		this.fileWriter.close();
		this.secondarySeekListWriter.close();
	}
}
