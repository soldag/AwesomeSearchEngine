package indexer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

import org.javatuples.Pair;

public class IndexWriter implements AutoCloseable {
		
	public static final String TOKEN_POSTINGS_SEPARATOR = "|";
	public static final String POSTINGS_SEPARATOR = ";";
	public static final String POSTING_ENTRIES_SEPARATOR = ",";
	
	private RandomAccessFile fileWriter;
	private SeekListWriter seekListWriter;
	
	public IndexWriter(File indexFile) throws UnsupportedEncodingException, FileNotFoundException {
		this(indexFile, null);
	}
	
	public IndexWriter(File indexFile, File seekListFile) throws UnsupportedEncodingException, FileNotFoundException {
		this.fileWriter = new RandomAccessFile(indexFile, "rw");
		if(seekListFile != null) {
			this.seekListWriter = new SeekListWriter(seekListFile);
		}
		else {
			this.seekListWriter = null;
		}
	}

	public void write(String token, List<Pair<String, Long>> postingList) throws IOException {
		// Serialize posting list
		String serializedPostingList = postingList.stream()
										.map(x -> String.format("[%s%s%s]", x.getValue0(), POSTING_ENTRIES_SEPARATOR, x.getValue1()))
										.collect(Collectors.joining(POSTINGS_SEPARATOR));
		
		// Write line to file
		this.writeLine(token + TOKEN_POSTINGS_SEPARATOR + serializedPostingList);
		
		if(this.seekListWriter != null) {
			// Write seek list entry for the current token
			long offset = this.fileWriter.getFilePointer();
			this.seekListWriter.write(token, offset);
		}
	}
	
	private void writeLine(String line) throws IOException {
		line = line + System.getProperty("line.separator");
		this.fileWriter.write(line.getBytes());
	}
	
	public void close() throws IOException {
		this.seekListWriter.close();
		this.fileWriter.close();
	}
}
