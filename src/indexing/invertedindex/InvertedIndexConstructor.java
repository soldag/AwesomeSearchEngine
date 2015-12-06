package indexing.invertedindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import indexing.Token;
import postings.ContentType;
import postings.PostingTable;
import postings.TokenPostings;

public class InvertedIndexConstructor {

	/**
	 * Determines, whether the index should be compressed.
	 */
	private boolean compress;
	
	/**
	 * Contains the corresponding seek list, if it should be constructed.
	 */
	private InvertedIndexSeekList seekList;
	
	/**
	 * Contains the actual inverted index.
	 */
	private PostingTable invertedIndex = new PostingTable();
	
	
	/**
	 * Creates a new InvertedIndexConstructor instance, that does not create a seek list.
	 * @param compress
	 */
	public InvertedIndexConstructor(boolean compress) {
		this(compress, null);
	}
	
	/**
	 * Creates a new InvertedIndexConstructor instance, that creates a seek list.
	 * @param compress
	 * @param seekList
	 */
	public InvertedIndexConstructor(boolean compress, InvertedIndexSeekList seekList) {
		this.compress = compress;
		this.seekList = seekList;
	}
	
	
	/**
	 * Adds a new posting to index.
	 * @param documentId
	 * @param token
	 * @param contentType
	 * @param position
	 */
	public void add(int documentId, String token, ContentType contentType, int position) {
		this.invertedIndex.put(token, documentId, contentType, position);
	}
	
	/**
	 * Writes index to file. 
	 * @param indexFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void writeToFile(File indexFile) throws FileNotFoundException, IOException {
		this.writeToFile(indexFile, null);
	}
	
	/**
	 * Writes index and seek list to file.
	 * @param indexFile
	 * @param seekListFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void writeToFile(File indexFile, File seekListFile) throws FileNotFoundException, IOException {
		// Determine, if seek list should be created
		boolean createSeekList = seekListFile != null && this.seekList != null;
		
		// Sort tokens
		String[] sortedTokens = this.invertedIndex.tokenSet().stream().sorted().toArray(String[]::new);		
		
		// Open temporary file
		try (RandomAccessFile indexWriter = new RandomAccessFile(indexFile, "rw")) {
			// Write term counts
			indexWriter.writeInt(this.invertedIndex.tokenSet().size());
			
			// Write postings for each token
			for(String token: sortedTokens) {				
				// Add seek list entry
				if(createSeekList) {
					this.seekList.put(token, (int)indexWriter.getFilePointer());
				}
				
				// Write token
				Token.write(token, indexWriter);
				
				// Write postings
				TokenPostings postings = this.invertedIndex.ofToken(token);
				postings.writeTo(indexWriter, this.compress);
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
	 * Gets the number of tokens in the inverted index.
	 * @return
	 */
	public int size() {
		return this.invertedIndex.size();
	}
	
	/**
	 * Deletes all entries of the inverted index.
	 */
	public void clear() {
		this.invertedIndex.clear();
		this.seekList.clear();
	}
}
