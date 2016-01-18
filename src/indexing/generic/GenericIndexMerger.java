package indexing.generic;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import io.FileReaderWriterFactory;
import io.index.IndexReader;
import io.index.IndexWriter;

public abstract class GenericIndexMerger<K extends Comparable<K>, V> {
	
	/**
	 * Determines, whether the index files are compressed or not.
	 */
	private boolean isCompressed;
	
	
	public GenericIndexMerger(boolean isCompressed) {
		this.isCompressed = isCompressed;
	}
	
	
	public void merge(File destinationIndexFile, List<File> temporaryIndexFiles, File seekListFile) throws IOException {
		// Initialize seeklist
		GenericSeekList<K> seekList = this.newSeeklist();
		
		// Create destination index file
		try (IndexWriter destinationFile = FileReaderWriterFactory.getInstance().getDirectIndexWriter(destinationIndexFile, this.isCompressed)) {
			// Open temporary index files
			List<K> firstKeys = new ArrayList<K>(temporaryIndexFiles.size());
			List<IndexReader> sourceFiles = new ArrayList<IndexReader>(temporaryIndexFiles.size());
			int totalSize = 0;
			for(File temporaryIndexFile: temporaryIndexFiles) {
				IndexReader tempFile = FileReaderWriterFactory.getInstance().getBufferedIndexReader(temporaryIndexFile, this.isCompressed);
				sourceFiles.add(tempFile);
				totalSize += tempFile.readInt();
				firstKeys.add(this.readKey(tempFile));
			}
			
			// Write total positions count
			destinationFile.writeInt(totalSize);
			
			K lastKey = null;
			V lastValue = null;
			while(firstKeys.size() > 0) {
				// Get first key (sorted) and corresponding postings
				int nextKeyIndex = IntStream.range(0, firstKeys.size())	
										.boxed()
										.min(Comparator.comparing(x -> firstKeys.get(x)))
										.get();
				IndexReader currentFile = sourceFiles.get(nextKeyIndex);
				
				// Get key and corresponding value
				K key = firstKeys.get(nextKeyIndex);
				V value = this.readValue(key, currentFile);
				
				if(lastKey != null && lastKey.equals(key) && lastValue != null) {
					// Key was already read from another file, so merge values
					lastValue = this.mergeValues(lastValue, value);
				}
				else {
					// New key was read, to write saved key and corresponding value to file
					if(lastKey != null && lastValue != null) {
						this.write(destinationFile, lastKey, lastValue, seekList);
					}

					// Save current key and its value for the case, that there are more values for this key in other files
					lastKey = key;
					lastValue = value;
				}				
				
				// Refresh tokens and source files list
				if(currentFile.getFilePointer() < currentFile.length()) {
					firstKeys.set(nextKeyIndex, this.readKey(sourceFiles.get(nextKeyIndex)));
				}
				else {
					currentFile.close();
					sourceFiles.remove(nextKeyIndex);
					firstKeys.remove(nextKeyIndex);
					
					// If end of last file was reached, write remaining entry to file
					if(sourceFiles.isEmpty()) {
						this.write(destinationFile, lastKey, lastValue, seekList);
					}
				}
			}
		}
		
		// Write seek list to file
		try(IndexWriter seekListWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(seekListFile, this.isCompressed)) {
			seekList.save(seekListWriter);
		}
	}
	
	private void write(IndexWriter indexWriter, K key, V value, GenericSeekList<K> seekList) throws UnsupportedEncodingException, IOException {
		// Add token to seek list
		seekList.put(key, (int)indexWriter.getFilePointer());
		
		// Write to destination file
		this.writeKey(key, indexWriter);
		this.writeValue(value, indexWriter);
	}
	
	protected abstract K readKey(IndexReader indexReader) throws IOException;
	
	protected abstract void writeKey(K key, IndexWriter indexWriter) throws IOException;
	
	protected abstract V readValue(K key, IndexReader indexReader) throws IOException;
	
	protected abstract void writeValue(V value, IndexWriter indexWriter) throws IOException;
	
	protected abstract V mergeValues(V value1, V value2);
	
	protected abstract GenericSeekList<K> newSeeklist();
}
