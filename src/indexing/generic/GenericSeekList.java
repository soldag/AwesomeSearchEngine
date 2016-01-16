package indexing.generic;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.index.IndexReader;
import io.index.IndexWriter;

public abstract class GenericSeekList<K extends Comparable<K>> {
	
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
	private TreeMap<K, Integer> seekList = new TreeMap<K, Integer>();
	
	
	/**
	 * Creates a new GenericSeekList instance.
	 * @param skip_number
	 */
	protected GenericSeekList(int skip_number) {
		this.skipNumber = skip_number;
	}
	
	
	/**
	 * Gets the set of keys of the seek list.
	 * @return
	 */
	public Set<K> keySet() {
		return this.seekList.keySet();
	}
	
	/**
	 * Gets the start offset of the corresponding index file for finding the specified key.
	 * @param key
	 * @return
	 */
	public int get(K key) {
		Map.Entry<K, Integer> entry = this.seekList.floorEntry(key);
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
	public void put(K key, int value) {
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
	
	/**
	 * Loads the seek list from a specified FileReader.
	 */
	public abstract void load(IndexReader reader) throws IOException;
	
	/**
	 * Saves the seek list to a specified FileWriter.
	 */
	public abstract void save(IndexWriter writer) throws IOException;
}
