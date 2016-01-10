package indexing.generic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.FileFactory;
import io.FileWriter;

public abstract class GenericIndexConstructor<T extends Comparable<T>> {
	
	/**
	 * Determines, whether the index should be compressed.
	 */
	private boolean compress;
	
	/**
	 * Contains the corresponding seek list, if it should be constructed.
	 */
	private GenericSeekList<T> seekList;
	
	
	/**
	 * Creates a new GenericIndexConstructor instance, that does not create a seek list.
	 * @param compress
	 */
	protected GenericIndexConstructor(boolean compress) {
		this(compress, null);
	}
	
	/**
	 * Creates a new GenericIndexConstructor instance, that creates a seek list.
	 * @param compress
	 * @param seekList
	 */
	protected GenericIndexConstructor(boolean compress, GenericSeekList<T> seekList) {
		this.compress = compress;
		this.seekList = seekList;
	}
	
	
	/**
	 * Write index to file.
	 * @param indexFile
	 * @throws IOException
	 */
	public void writeToFile(File indexFile) throws IOException {
		this.writeToFile(indexFile, null);
	}
	
	/**
	 * Writes index and seek list to file.
	 * @param indexFile
	 * @param seekListFile
	 * @throws IOException
	 */
	public void writeToFile(File indexFile, File seekListFile) throws IOException {
		// Determine, if seek list should be created
		boolean createSeekList = seekListFile != null && this.seekList != null;	
		
		// Open index file
		try (FileWriter indexWriter = FileFactory.getInstance().getWriter(indexFile, this.compress)) {
			// Write entries counts
			indexWriter.writeInt(this.keys().size());
			
			// Write values for each index entry
			List<T> sortedKeys = this.keys().stream().sorted().collect(Collectors.toList());
			for(T key: sortedKeys) {				
				// Add seek list entry
				if(createSeekList) {
					this.seekList.put(key, (int)indexWriter.getFilePointer());
				}
				
				// Write entry
				this.writeEntry(key, indexWriter);
			}
		}
		
		// Write seek list to file
		if(createSeekList) {
			try(FileWriter seekListWriter = FileFactory.getInstance().getWriter(seekListFile, this.compress)) {
				this.seekList.save(seekListWriter);
			}
		}
	}	
	
	/**
	 * Gets the set of keys of the index entries.
	 * @return
	 */
	protected abstract Set<T> keys();
	
	/**
	 * Writes index entry identified by its key to file using the given file writer.
	 * @param key
	 * @param indexWriter
	 */
	protected abstract void writeEntry(T key, FileWriter indexWriter) throws IOException;

	/**
	 * Gets the number of entries in the index.
	 * @return
	 */
	public abstract int size();
	
	/**
	 * Deletes all entries of the index.
	 */
	public void clear() {
		this.seekList.clear();
	}
}
