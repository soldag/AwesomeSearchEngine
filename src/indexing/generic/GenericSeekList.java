package indexing.generic;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

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
	protected TreeMap<T, Integer> seekList = new TreeMap<T, Integer>();
	
	
	/**
	 * Creates a new GenericSeekList instance.
	 * @param skip_number
	 */
	protected GenericSeekList(int skip_number) {
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
		Map.Entry<T, Integer> entry = this.seekList.floorEntry(key);
		if(entry != null) {
			return entry.getValue();
		}
		
		return -1;
	}
	
	/**
	 * Adds new entry to seek list. Entries have to be put into the seek list in the order in which they should be written to file.
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
