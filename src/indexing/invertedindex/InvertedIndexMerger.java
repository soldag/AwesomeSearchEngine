package indexing.invertedindex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import io.FileReaderWriterFactory;
import io.index.IndexReader;
import io.index.IndexWriter;
import postings.TokenPostings;

public class InvertedIndexMerger {
	
	/**
	 * Determines, whether the index files are compressed or not.
	 */
	private boolean isCompressed;
	
	
	/**
	 * Creates a new InvertedIndexMerger instance.
	 * @param isCompressed
	 */
	public InvertedIndexMerger(boolean isCompressed) {
		this.isCompressed = isCompressed;
	}
	
	
	public void merge(File frequencyIndexFile, File positionalIndexFile, List<Pair<File, File>> temporaryIndexFiles, File seekListFile) throws IOException {
		// Initialize seeklist
		InvertedIndexSeekList seekList = new InvertedIndexSeekList();
		
		// Create destination index files
		try (IndexWriter frequencyIndexWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(frequencyIndexFile, this.isCompressed);
			 IndexWriter positionalIndexWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(positionalIndexFile, this.isCompressed)) {
			// Open temporary index files
			List<String> firstTokens = new ArrayList<String>(temporaryIndexFiles.size());
			List<IndexReader> frequencyIndexReaders = new ArrayList<IndexReader>(temporaryIndexFiles.size());
			List<IndexReader> positionalIndexReaders = new ArrayList<IndexReader>(temporaryIndexFiles.size());
			int totalSize = 0;
			for(Pair<File, File> tempFilePair: temporaryIndexFiles) {
				IndexReader tempFrequencyIndexReader = FileReaderWriterFactory.getInstance().getBufferedIndexReader(tempFilePair.getLeft(), this.isCompressed);
				IndexReader tempPositionalIndexReader = FileReaderWriterFactory.getInstance().getBufferedIndexReader(tempFilePair.getRight(), this.isCompressed);
				frequencyIndexReaders.add(tempFrequencyIndexReader);
				positionalIndexReaders.add(tempPositionalIndexReader);
				
				totalSize += tempFrequencyIndexReader.readInt();
				firstTokens.add(tempFrequencyIndexReader.readString());
			}
			
			// Write total positions count
			frequencyIndexWriter.writeInt(totalSize);
			
			String lastToken = null;
			TokenPostings lastPostings = null;
			while(firstTokens.size() > 0) {
				// Determine next token
				int nextTokenIndex = IntStream.range(0, firstTokens.size())	
										.boxed()
										.min(Comparator.comparing(x -> firstTokens.get(x)))
										.get();
				IndexReader currentFrequencyReader = frequencyIndexReaders.get(nextTokenIndex);
				IndexReader currentPositionalReader = positionalIndexReaders.get(nextTokenIndex);
				
				// Get token and corresponding postings
				String token = firstTokens.get(nextTokenIndex);
				TokenPostings postings = TokenPostings.load(currentFrequencyReader.getSkippingAreaReader(), currentPositionalReader, true);
				
				if(lastToken != null && lastToken.equals(token) && lastPostings != null) {
					// Token was already read from another file, so merge postings
					lastPostings.putAll(postings);
				}
				else {
					// New token was read, to write saved token and corresponding postings to file
					if(lastToken != null && lastPostings != null) {
						this.write(frequencyIndexWriter, positionalIndexWriter, lastToken, lastPostings, seekList);
					}

					// Save current token and its postings for the case, that there are more values for this token in other files
					lastToken = token;
					lastPostings = postings;
				}				
				
				// Refresh tokens and source files list
				if(currentFrequencyReader.getFilePointer() < currentFrequencyReader.length()) {
					firstTokens.set(nextTokenIndex, frequencyIndexReaders.get(nextTokenIndex).readString());
				}
				else {
					currentFrequencyReader.close();
					frequencyIndexReaders.remove(nextTokenIndex);
					currentPositionalReader.close();
					positionalIndexReaders.remove(nextTokenIndex);
					firstTokens.remove(nextTokenIndex);
					
					// If end of last file was reached, write remaining entry to file
					if(frequencyIndexReaders.isEmpty()) {
						this.write(frequencyIndexWriter, positionalIndexWriter, lastToken, lastPostings, seekList);
					}
				}
			}
		}
		
		// Write seek list to file
		try(IndexWriter seekListWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(seekListFile, this.isCompressed)) {
			seekList.save(seekListWriter);
		}
	}
	
	/**
	 * Write index entry to file and add seek list entry
	 * @param frequencyIndexWriter
	 * @param positionalIndexWriter
	 * @param token
	 * @param postings
	 * @param seekList
	 * @throws IOException
	 */
	private void write(IndexWriter frequencyIndexWriter, IndexWriter positionalIndexWriter, String token, TokenPostings postings, InvertedIndexSeekList seekList) throws IOException {
		// Add token to seek list
		seekList.put(token, frequencyIndexWriter.getFilePointer());
		
		// Write to destination file
		frequencyIndexWriter.writeString(token);
		frequencyIndexWriter.startSkippingArea();
		postings.save(frequencyIndexWriter, positionalIndexWriter);
		frequencyIndexWriter.endSkippingArea();
	}	
}
