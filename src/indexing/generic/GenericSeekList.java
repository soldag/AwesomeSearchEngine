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
	private TreeMap<K, Long> seekList = new TreeMap<K, Long>();
	
	
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
	public long get(K key) {
		Map.Entry<K, Long> entry = this.seekList.floorEntry(key);
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
	public void put(K key, long value) {
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
	 * Loads the seek list from a specified IndexReader.
	 */
	public void load(IndexReader reader) throws IOException {
		this.clear();
		
		while(reader.getFilePointer() < reader.length()) {
			K key = this.readKey(reader);
			long offset = reader.readLong();
			
			this.seekList.put(key, offset);
		}
	}
	
	/**
	 * Saves the seek list to a specified IndexWriter.
	 */
	public void save(IndexWriter writer) throws IOException {
		for(Map.Entry<K, Long> entry: this.seekList.entrySet()) {
			this.writeKey(entry.getKey(), writer);
			writer.writeLong(entry.getValue());
		}
	}

	/**
	 * Reads a key from the specified IndexReader.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	protected abstract K readKey(IndexReader reader) throws IOException;
	
	/**
	 * Writes the given key to the specified IndexWriter.
	 * @param key
	 * @param writer
	 * @throws IOException
	 */
	protected abstract void writeKey(K key, IndexWriter writer) throws IOException;
}
