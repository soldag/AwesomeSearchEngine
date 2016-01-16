package indexing.invertedindex;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import postings.PostingTable;
import postings.TokenPostings;

import io.FileReaderWriterFactory;
import io.index.IndexReader;

public class InvertedIndexReader implements AutoCloseable {
	
	/**
	 * Contains the file reader for the index file.
	 */
	private IndexReader indexFile;
	
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
		this.indexFile = FileReaderWriterFactory.getInstance().getDirectIndexReader(indexFile, isCompressed);		
		this.totalTokenCount = this.indexFile.readInt();
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
				String readToken = this.indexFile.readString();
				int postingsLength = this.indexFile.readInt();
				
				if(prefixSearch) {
					if(readToken.startsWith(token)) {
						TokenPostings readPostings = TokenPostings.load(this.indexFile, postingsLength);
						postings.putAll(readToken, readPostings);
						continue;
					}
				}			
				else if(readToken.equals(token)) {
					TokenPostings readPostings = TokenPostings.load(this.indexFile, postingsLength);
					postings.putAll(readToken, readPostings);
					break;
				}
				
				if(readToken.compareTo(token) > 0){
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