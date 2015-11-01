package indexer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.javatuples.Pair;

public class IndexWriter implements AutoCloseable {
		
	public static final String TOKEN_POSTINGS_SEPARATOR = "|";
	public static final String POSTINGS_SEPARATOR = ";";
	public static final String POSTING_ENTRIES_SEPARATOR = ",";
	
	private RandomAccessFile fileWriter;
	private SeekListWriter seekListWriter;
	private boolean compressed;
	
	public IndexWriter(File indexFile, boolean compressed) throws UnsupportedEncodingException, FileNotFoundException {
		this(indexFile, null, null, compressed);
	}
	
	public IndexWriter(File indexFile, File seekListFile, File secondarySeekListFile, boolean compressed) throws UnsupportedEncodingException, FileNotFoundException {
		this.fileWriter = new RandomAccessFile(indexFile, "rw");
		if(seekListFile != null) {
			this.seekListWriter = new SeekListWriter(seekListFile, secondarySeekListFile);
		}
		else {
			this.seekListWriter = null;
		}
		this.compressed = compressed;
	}

	public void write(String token, List<Pair<Integer, Long>> postingList) throws IOException {
		String serializedPostingList = "";
		// Serialize posting list
		if(this.compressed) {
			HashMap<Integer, List<Long>> groupedEntries = new LinkedHashMap<Integer, List<Long>>();
			Long lastOffset = (long) 0;
			int lastDocId = 0;
			
			// Sort postingList after DocID
			postingList = postingList.stream().sorted((x1, x2) -> x1.getValue0().compareTo(x2.getValue0())).collect(Collectors.toList());
			
			for(Pair<Integer, Long> tuple: postingList){
				if(!groupedEntries.containsKey(tuple.getValue0())){
					groupedEntries.put(tuple.getValue0(), new ArrayList<Long>());
					lastOffset = (long) 0;
				}
				groupedEntries.get(tuple.getValue0()).add(tuple.getValue1() - lastOffset);
				lastOffset = tuple.getValue1();
			}
			
			for(Map.Entry<Integer, List<Long>> entry :groupedEntries.entrySet()) {
				int deltaDocId = entry.getKey() - lastDocId;
				String values = entry.getValue().stream().map(x -> x.toString()).collect(Collectors.joining(POSTING_ENTRIES_SEPARATOR));
				serializedPostingList += "[" + deltaDocId + POSTING_ENTRIES_SEPARATOR + "[" + values + "]]" + POSTINGS_SEPARATOR;
				lastDocId = entry.getKey();
			}
			serializedPostingList = serializedPostingList.substring(0, serializedPostingList.length() - 1);
			
		}
		else {
			serializedPostingList = postingList.stream()
											.map(x -> String.format("[%s%s%s]", x.getValue0(), POSTING_ENTRIES_SEPARATOR, x.getValue1()))
											.collect(Collectors.joining(POSTINGS_SEPARATOR));
		
		}
		
		if(this.seekListWriter != null) {
			// Write seek list entry for the current token
			long offset = this.fileWriter.getFilePointer();
			this.seekListWriter.write(token, offset);
		}
		
		// Write line to file
		this.writeLine(token + TOKEN_POSTINGS_SEPARATOR + serializedPostingList);
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
