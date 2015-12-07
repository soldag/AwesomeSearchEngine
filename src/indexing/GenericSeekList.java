package indexing;

import java.io.IOException;
import java.util.LinkedHashMap;

import io.FileReader;
import io.FileWriter;

public abstract class GenericSeekList<T extends Comparable<T>> {
	
	/**
	 * Determines, how many entries are skipped in between two seek list entries.
	 */
	private int skipNumber;
	
	/**
	 * Contains the number of entries in the seek list.
	 */
	private int totalEntriesCount = 0;
	
	/**
	 * Contains the actual seek list in memory.
	 */
	protected LinkedHashMap<T, Integer> seekList = new LinkedHashMap<T, Integer>();
	
	
	/**
	 * Creates a new GenericSeekList instance.
	 * @param skip_number
	 */
	public GenericSeekList(int skip_number) {
		this.skipNumber = skip_number;
	}
	
	
	/**
	 * Loads the seek list from a specified FileReader.
	 */
	public abstract void load(FileReader reader) throws IOException;
	
	/**
	 * Saves the seek list to a specified FileWriter.
	 */
	public abstract void save(FileWriter writer) throws IOException;
	
	/**
	 * Gets the start offset of the corresponding index file for finding the specified key.
	 * @param key
	 * @return
	 */
	public int getIndexOffset(T key) {
		// Direct match: seek list contains token
		if(this.seekList.containsKey(key)) {
			return this.seekList.get(key);
		}
		
		// Iterate over seek list and determine the nearest predecessor and successor of the token
		int startOffset = -1;
		for(T entryToken: this.seekList.keySet()) {
			if(key.compareTo(entryToken) < 0) {
				if(startOffset >= 0) {
					break;
				}
				else {
					// Already the first token in the seek list (which is also the first one in the index) is bigger, than the token searched-for.
					// This means, the index does not contain this token.
					return -1;
				}
			}
			startOffset = this.seekList.get(entryToken);
		}
		
		// Token could only be appear after the last token of the seek list in index file, thus no end offset can be specified.
		return startOffset;
	}
	
	/**
	 * Adds new entry to seek list.
	 * @param key
	 * @param value
	 */
	public void put(T key, int value) {
		if(this.totalEntriesCount % this.skipNumber == 0) {
			this.seekList.put(key, value);
		}
		
		this.totalEntriesCount++;
	}
	
	/**
	 * Removes all entries of the seek list.
	 */
	public void clear() {
		this.seekList.clear();
	}
}
