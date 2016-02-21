package indexing.generic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.FileReaderWriterFactory;
import io.index.IndexWriter;

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
	 * Determines, whether the index should be compressed.
	 * @return
	 */
	public boolean isCompressed() {
		return this.compress;
	}
	
	
	/**
	 * Write index to file.
	 * @param indexFile
	 * @throws IOException
	 */
	public void save(File indexFile) throws IOException {
		this.saveWithSeekList(indexFile, null);
	}
	
	/**
	 * Writes index and seek list to file.
	 * @param indexFile
	 * @param seekListFile
	 * @throws IOException
	 */
	public void saveWithSeekList(File indexFile, File seekListFile) throws IOException {
		// Determine, if seek list should be created
		boolean createSeekList = seekListFile != null && this.seekList != null;	
		
		// Open index file
		try (IndexWriter indexWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(indexFile, this.isCompressed())) {
			// Write size
			indexWriter.writeInt(this.size());
			
			// Write values for each index entry
			List<T> sortedKeys = this.keys().stream().sorted().collect(Collectors.toList());
			for(T key: sortedKeys) {				
				// Add seek list entry
				if(createSeekList) {
					this.seekList.put(key, indexWriter.getFilePointer());
				}
				
				// Write entry
				this.writeEntry(key, indexWriter);
			}
		}
		
		// Write seek list to file
		if(createSeekList) {
			try(IndexWriter seekListWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(seekListFile, this.isCompressed())) {
				this.seekList.save(seekListWriter);
			}
		}
	}	
	
	/**
	 * Gets the set of keys of the index entries.
	 * @return
	 */
	public abstract Set<T> keys();
	
	/**
	 * Writes index entry identified by its key to file using the given file writer.
	 * @param key
	 * @param indexWriter
	 */
	protected abstract void writeEntry(T key, IndexWriter indexWriter) throws IOException;

	/**
	 * Gets the size of the index.
	 * @return
	 */
	public abstract int size();
	
	/**
	 * Gets the number of entries in the index.
	 */
	public abstract int entriesCount();
	
	/**
	 * Deletes all entries of the index.
	 */
	public void clear() {
		this.seekList.clear();
	}
}
