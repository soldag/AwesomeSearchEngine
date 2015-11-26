package indexing.invertedindex;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import indexing.Posting;
import indexing.Token;

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
			byte[] lastPostingsBytes = null;
			while(firstTokens.size() > 0) {
				// Get first token (alphabetically) and corresponding postings
				int nextTokenIndex = IntStream.range(0, firstTokens.size())	
										.boxed()
										.min(Comparator.comparing(x -> firstTokens.get(x)))
										.get();
				RandomAccessFile currentFile = sourceFiles.get(nextTokenIndex);
				
				// Get token and postings
				String token = firstTokens.get(nextTokenIndex);
				byte[] postingsBytes = this.readPostingsBytes(currentFile);
				
				if(lastToken.equals(token) && lastPostingsBytes != null) {
					// Token was already read from another file, so merge postings
					byte[] mergedPostingsBytes = this.mergePostingLists(lastPostingsBytes, postingsBytes);
					lastPostingsBytes = mergedPostingsBytes;
				}
				else {
					// New token was read, to write saved token and corresponding postings to file
					if(lastPostingsBytes != null) {
						this.write(destinationFile, lastToken, lastPostingsBytes, createSeekList);
					}

					// Save current token and its postings for the case, that there are more postings for this token in other files
					lastToken = token;
					lastPostingsBytes = postingsBytes;
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
						this.write(destinationFile, lastToken, lastPostingsBytes, createSeekList);
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
	 * Reads posting list from specified file.
	 * @param indexFile
	 * @return posting list as byte array
	 * @throws IOException
	 */
	private byte[] readPostingsBytes(RandomAccessFile indexFile) throws IOException {
		int postingsLength = indexFile.readInt();
		byte[] postingsBytes = new byte[postingsLength];
		indexFile.readFully(postingsBytes);
		
		return postingsBytes;
	}
	
	/**
	 * Merges two byte arrays containing posting lists.
	 * @param postingsBytes1
	 * @param postingsBytes2
	 * @return merged posting list
	 * @throws IOException
	 */
	private byte[] mergePostingLists(byte[] postingsBytes1, byte[] postingsBytes2) throws IOException {
		// Parse, concatenate and sort postings
		List<Posting> mergedPostings = Stream
				.concat(
					Posting.fromBytes(postingsBytes1, this.isCompressed).stream(), 
					Posting.fromBytes(postingsBytes2, this.isCompressed).stream()
				)
				.sorted()
				.collect(Collectors.toList());
		
		// Serialize postings back to bytes
		ByteBuffer buffer =  ByteBuffer.allocate(postingsBytes1.length + postingsBytes2.length);
		int lastDocumentId = 0;
		for(Posting posting: mergedPostings) {
			buffer.put(posting.toBytes(this.isCompressed, lastDocumentId));
			lastDocumentId = posting.getDocumentId();
		}
		
		return buffer.array();
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
	private void write(RandomAccessFile file, String token, byte[] postingBytes, boolean createSeekList) throws UnsupportedEncodingException, IOException {
		// Add token to seek list
		if(createSeekList) {
			this.seekList.put(token, (int)file.getFilePointer());
		}
		
		// Write to destination file
		Token.write(token, file);
		file.writeInt(postingBytes.length);
		file.write(postingBytes);
	}
}
