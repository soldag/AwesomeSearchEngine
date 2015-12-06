package indexing.invertedindex;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


import indexing.Token;
import postings.PostingTable;
import postings.TokenPostings;

public class InvertedIndexReader implements AutoCloseable {
	
	/**
	 * Contains the file reader for the index file.
	 */
	private RandomAccessFile indexFile;
	
	/**
	 * Determines, whether the index files, that should be merged, are compressed or not.
	 */
	private boolean isCompressed;
	
	/**
	 * Contains the number of all tokens occurrences in the index.
	 */
	private int totalTokenCount = 0;
	
	
	/**
	 * Creates a new InvertedIndexReader instance.
	 * @param indexFile
	 * @param isCompressed
	 * @throws IOException
	 */
	public InvertedIndexReader(File indexFile, boolean isCompressed) throws IOException {
		this.indexFile = new RandomAccessFile(indexFile, "r");
		this.totalTokenCount = this.indexFile.readInt();
		this.isCompressed = isCompressed;
	}
	
	
	/**
	 * Gets the number of all tokens occurrences in the index.
	 * @return
	 */
	public int getTotalTokenCount() {
		return this.totalTokenCount;
	}


	/**
	 * Gets a map of postings per token from inverted index by specifying a start offset in the index file. 
	 * Additionally, prefix search can be enabled. In this case, all tokens, that start with the given token, are also taken into account.
	 * @param token
	 * @param startOffset
	 * @param prefixSearch
	 * @return List of postings
	 * @throws IOException
	 */
	public PostingTable getPostings(String token, int startOffset, boolean prefixSearch) throws IOException {
		PostingTable postings = new PostingTable();
		
		this.indexFile.seek(startOffset);
		while(true) {
			try {
				String readToken = Token.read(this.indexFile);
				int postingsLength = this.indexFile.readInt();
				
				if(prefixSearch) {
					if(readToken.startsWith(token)) {
						TokenPostings readPostings = TokenPostings.readFrom(this.indexFile, postingsLength, this.isCompressed);
						postings.putAll(readToken, readPostings);
						continue;
					}
					else if(readToken.compareTo(token) > 0){
						break;
					}
				}			
				else if(readToken.equals(token)) {
					TokenPostings readPostings = TokenPostings.readFrom(this.indexFile, postingsLength, this.isCompressed);
					postings.putAll(readToken, readPostings);
					break;
				}
				
				this.indexFile.seek(this.indexFile.getFilePointer() + postingsLength);
			}
			catch(EOFException e) {
				break;
			}
		}
		
		return postings;
	}
	
	/**
	 * Closes this resource, relinquishing any underlying resources.
	 */
	public void close() throws IOException {
		this.indexFile.close();
	}	
}