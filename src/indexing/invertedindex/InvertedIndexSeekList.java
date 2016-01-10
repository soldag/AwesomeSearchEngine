package indexing.invertedindex;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import indexing.generic.GenericSeekList;
import io.FileReader;
import io.FileWriter;

public class InvertedIndexSeekList extends GenericSeekList<String> {
	
	/**
	 * Determines, how many documents are skipped in between two seek list entries.
	 */
	private static final int SKIP_NUMBER = 200;
	
	
	/**
	 * Creates a new IndexSeekList instance.
	 */
	public InvertedIndexSeekList() {
		super(SKIP_NUMBER);
	}
	
	
	/**
	 * Loads the seek list from a specified FileReader.
	 */
	public void load(FileReader reader) throws IOException {
		this.seekList.clear();
		
		while(reader.getFilePointer() < reader.length()) {
			String token = reader.readString();
			int offset = reader.readInt();				
			this.seekList.put(token, offset);
		}
	}
	
	/**
	 * Saves the seek list to a specified FileWriter.
	 */
	public void save(FileWriter writer) throws IOException {
		// Sort tokens alphabetically
		List<String> tokens = this.seekList.keySet().stream().sorted().collect(Collectors.toList());
		
		for(String token: tokens) {
			writer.writeString(token);
			writer.writeInt(this.seekList.get(token));
		}
	}
}
