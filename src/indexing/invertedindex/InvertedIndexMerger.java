package indexing.invertedindex;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import indexing.generic.IndexMerger;
import io.FileReaderWriterFactory;
import io.index.IndexReader;
import io.index.IndexWriter;
import postings.TokenPostings;

public class InvertedIndexMerger implements IndexMerger {
	
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
	
	
	@Override
	public void merge(File destinationIndexFile, List<File> temporaryIndexFiles, File seekListFile) throws IOException {
		// Initialize seeklist
		InvertedIndexSeekList seekList = new InvertedIndexSeekList();
		
		// Create destination index file
		try (IndexWriter destinationFile = FileReaderWriterFactory.getInstance().getDirectIndexWriter(destinationIndexFile, this.isCompressed)) {
			// Open temporary index files
			List<String> firstTokens = new ArrayList<String>(temporaryIndexFiles.size());
			List<IndexReader> sourceFiles = new ArrayList<IndexReader>(temporaryIndexFiles.size());
			long totalPositionsCount = 0;
			for(File temporaryIndexFile: temporaryIndexFiles) {
				IndexReader tempFile = FileReaderWriterFactory.getInstance().getBufferedIndexReader(temporaryIndexFile, this.isCompressed);
				sourceFiles.add(tempFile);
				totalPositionsCount += tempFile.readInt();
				firstTokens.add(tempFile.readString());
			}
			
			// Write total positions count
			destinationFile.uncompressed().writeLong(totalPositionsCount);
			
			String lastToken = null;
			TokenPostings lastPostings = null;
			while(firstTokens.size() > 0) {
				// Get first token (alphabetically) and corresponding postings
				int nextTokenIndex = IntStream.range(0, firstTokens.size())	
										.boxed()
										.min(Comparator.comparing(x -> firstTokens.get(x)))
										.get();
				IndexReader currentFile = sourceFiles.get(nextTokenIndex);
				
				// Get token and postings
				String token = firstTokens.get(nextTokenIndex);
				TokenPostings postings = TokenPostings.load(currentFile);
				
				if(lastToken != null && lastToken.equals(token) && lastPostings != null) {
					// Token was already read from another file, so merge postings
					lastPostings.putAll(postings);
				}
				else {
					// New token was read, to write saved token and corresponding postings to file
					if(lastToken != null && lastPostings != null) {
						this.write(destinationFile, lastToken, lastPostings, seekList);
						totalPositionsCount += lastPostings.totalOccurencesCount();
					}

					// Save current token and its postings for the case, that there are more values for this token in other files
					lastToken = token;
					lastPostings = postings;
				}				
				
				// Refresh tokens and source files list
				if(currentFile.getFilePointer() < currentFile.length()) {
					firstTokens.set(nextTokenIndex, sourceFiles.get(nextTokenIndex).readString());
				}
				else {
					currentFile.close();
					sourceFiles.remove(nextTokenIndex);
					firstTokens.remove(nextTokenIndex);
					
					// If end of last file was reached, write remaining entry to file
					if(sourceFiles.isEmpty()) {
						this.write(destinationFile, lastToken, lastPostings, seekList);
						totalPositionsCount += lastPostings.totalOccurencesCount();
					}
				}
			}
		}
		
		// Write seek list to file
		try(IndexWriter seekListWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(seekListFile, this.isCompressed)) {
			seekList.save(seekListWriter);
		}
	}
	
	private void write(IndexWriter indexWriter, String token, TokenPostings postings, InvertedIndexSeekList seekList) throws UnsupportedEncodingException, IOException {
		// Add token to seek list
		seekList.put(token, (int)indexWriter.getFilePointer());
		
		// Write to destination file
		indexWriter.writeString(token);
		indexWriter.startSkippingArea();
		postings.save(indexWriter);
		indexWriter.endSkippingArea();
	}
}
