package indexing.invertedindex;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import postings.TokenPostings;
import io.FileFactory;
import io.FileReader;
import io.FileWriter;

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
		try (FileWriter destinationFile = FileFactory.getInstance().getWriter(destinationIndexFile, this.isCompressed)) {
			// Open temporary index files
			List<String> firstTokens = new ArrayList<String>(temporaryIndexFiles.size());
			List<FileReader> sourceFiles = new ArrayList<FileReader>(temporaryIndexFiles.size());
			int totalTermsCount = 0;
			for(File temporaryIndexFile: temporaryIndexFiles) {
				FileReader tempFile = FileFactory.getInstance().getReader(temporaryIndexFile, this.isCompressed);
				sourceFiles.add(tempFile);
				totalTermsCount += tempFile.readInt();
				firstTokens.add(tempFile.readString());
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
				FileReader currentFile = sourceFiles.get(nextTokenIndex);
				
				// Get token and postings
				String token = firstTokens.get(nextTokenIndex);
				TokenPostings postings = TokenPostings.load(currentFile);
				
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
					firstTokens.set(nextTokenIndex, sourceFiles.get(nextTokenIndex).readString());
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
			try(FileWriter seekListWriter = FileFactory.getInstance().getWriter(seekListFile, this.isCompressed)) {
				this.seekList.save(seekListWriter);
			}
		}
	}
	
	private void write(FileWriter writer, String token, TokenPostings postings, boolean createSeekList) throws UnsupportedEncodingException, IOException {
		// Add token to seek list
		if(createSeekList) {
			this.seekList.put(token, (int)writer.getFilePointer());
		}
		
		// Write to destination file
		writer.writeString(token);
		postings.save(writer);
	}
}
