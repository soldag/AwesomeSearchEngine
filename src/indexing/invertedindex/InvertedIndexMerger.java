package indexing.invertedindex;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import indexing.Token;
import postings.TokenPostings;

public class InvertedIndexMerger {
	
	/**
	 * Determines, whether the index files, that should be merged, are compressed or not.
	 */
	private final boolean isCompressed;
	
	/**
	 * Contains the corresponding seek list, if it should be constructed.
	 */
	private InvertedIndexSeekList seekList;
	
	
	/**
	 * Creates a new InvertedIndexMerger instance.
	 * @param isCompressed
	 */
	public InvertedIndexMerger(boolean isCompressed) {
		this(isCompressed, null);
	}
	
	/**
	 * Creates a new InvertedIndexMerger instance.
	 * @param isCompressed
	 * @param seekList
	 */
	public InvertedIndexMerger(boolean isCompressed, InvertedIndexSeekList seekList) {
		this.isCompressed = isCompressed;
		this.seekList = seekList;
	}
	
	
	/**
	 * Merges the given list of temporary index files to the specified destination index file, without creating a seek list.
	 * @param destinationIndexFile
	 * @param temporaryIndexFiles
	 * @throws IOException
	 */
	public void merge(File destinationIndexFile, List<File> temporaryIndexFiles) throws IOException {
		this.merge(destinationIndexFile, temporaryIndexFiles, null);
	}

	/**
	 * Merges the given list of temporary index files to the specified destination index file and creates a seek list for the newly created index.
	 * @param destinationIndexFile
	 * @param temporaryIndexFiles
	 * @param seekListFile
	 * @throws IOException
	 */
	public void merge(File destinationIndexFile, List<File> temporaryIndexFiles, File seekListFile) throws IOException {
		// Determine, if seek list should be created
		boolean createSeekList = seekListFile != null && this.seekList != null;
		
		// Create destination index file
		try (RandomAccessFile destinationFile = new RandomAccessFile(destinationIndexFile, "rw")) {
			// Open temporary index files
			List<String> firstTokens = new ArrayList<String>(temporaryIndexFiles.size());
			List<RandomAccessFile> sourceFiles = new ArrayList<RandomAccessFile>(temporaryIndexFiles.size());
			int totalTermsCount = 0;
			for(File temporaryIndexFile: temporaryIndexFiles) {
				RandomAccessFile tempFile = new RandomAccessFile(temporaryIndexFile, "r");
				sourceFiles.add(tempFile);
				totalTermsCount += tempFile.readInt();
				firstTokens.add(Token.read(tempFile));
			}
			
			// Write total terms count
			destinationFile.writeInt(totalTermsCount);
			
			String lastToken = "";
			TokenPostings lastPostings = null;
			while(firstTokens.size() > 0) {
				// Get first token (alphabetically) and corresponding postings
				int nextTokenIndex = IntStream.range(0, firstTokens.size())	
										.boxed()
										.min(Comparator.comparing(x -> firstTokens.get(x)))
										.get();
				RandomAccessFile currentFile = sourceFiles.get(nextTokenIndex);
				
				// Get token and postings
				String token = firstTokens.get(nextTokenIndex);
				TokenPostings postings = TokenPostings.readFrom(currentFile, this.isCompressed);
				
				if(lastToken.equals(token) && lastPostings != null) {
					// Token was already read from another file, so merge postings
					lastPostings.putAll(postings);
				}
				else {
					// New token was read, to write saved token and corresponding postings to file
					if(lastPostings != null) {
						this.write(destinationFile, lastToken, lastPostings, createSeekList);
					}

					// Save current token and its postings for the case, that there are more postings for this token in other files
					lastToken = token;
					lastPostings = postings;
				}				
				
				// Refresh tokens and source files list
				if(currentFile.getFilePointer() < currentFile.length()) {
					firstTokens.set(nextTokenIndex, Token.read(sourceFiles.get(nextTokenIndex)));
				}
				else {
					currentFile.close();
					sourceFiles.remove(nextTokenIndex);
					firstTokens.remove(nextTokenIndex);
					
					// If end of last file was reached, write remaining entry to file
					if(sourceFiles.isEmpty()) {
						this.write(destinationFile, lastToken, lastPostings, createSeekList);
					}
				}
			}
		}
		
		// Write seek list to file
		if(createSeekList) {
			try(RandomAccessFile seekListWriter = new RandomAccessFile(seekListFile, "rw")) {
				this.seekList.save(seekListWriter);
			}
		}
	}
	
	/**
	 * Writes index entry to specified file.
	 * @param file
	 * @param token
	 * @param postingBytes
	 * @param createSeekList
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private void write(RandomAccessFile file, String token, TokenPostings postings, boolean createSeekList) throws UnsupportedEncodingException, IOException {
		// Add token to seek list
		if(createSeekList) {
			this.seekList.put(token, (int)file.getFilePointer());
		}
		
		// Write to destination file
		Token.write(token, file);
		postings.writeTo(file, this.isCompressed);
	}
}
