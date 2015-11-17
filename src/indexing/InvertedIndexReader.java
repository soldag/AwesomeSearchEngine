package indexing;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import SearchEngine.Posting;

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
	 * Gets a list of postings from inverted index by specifying its id and a start offset in the map file. 
	 * Additionally, prefix search can be enabled. In this case, postings of all tokens, that start with the given token, are returned.
	 * @param token
	 * @param startOffset
	 * @param prefixSearch
	 * @return List of postings
	 * @throws IOException
	 */
	public List<Posting> getPostings(String token, int startOffset, boolean prefixSearch) throws IOException {
		List<Posting> postings = new ArrayList<Posting>();
		this.indexFile.seek(startOffset);
		while(true) {
			try {
				String readToken = Token.read(this.indexFile);
				int postingsLength = this.indexFile.readInt();
				
				if(prefixSearch) {
					if(readToken.startsWith(token)) {
						postings.addAll(this.readPostings(postingsLength));
						continue;
					}
					else if(readToken.compareTo(token) > 0){
						break;
					}
				}			
				else if(readToken.equals(token)) {
					postings = this.readPostings(postingsLength);
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
	 * Reads a posting from the current index file.
	 * @param length
	 * @return List of postings
	 * @throws IOException
	 */
	private List<Posting> readPostings(int length) throws IOException {
		byte[] postingsBytes = new byte[length];
		this.indexFile.readFully(postingsBytes);
		
		return Posting.fromBytes(postingsBytes, this.isCompressed);
	}
	
	/**
	 * Closes this resource, relinquishing any underlying resources.
	 */
	public void close() throws IOException {
		this.indexFile.close();
	}
}