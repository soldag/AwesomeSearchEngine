package indexing.invertedindex;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import indexing.Posting;
import indexing.Token;

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
	 * Gets a combined list of postings of the given token from inverted index by specifying a start offset in the index file. 
	 * Additionally, prefix search can be enabled. In this case, postings of all tokens, that start with the given token, are returned.
	 * @param token
	 * @param startOffset
	 * @param prefixSearch
	 * @return List of postings
	 * @throws IOException
	 */
	public List<Posting> getPostingsList(String token, int startOffset, boolean prefixSearch) throws IOException {
		return this.getPostingsMap(token, startOffset, prefixSearch).entrySet().stream()
				.flatMap(x -> x.getValue().stream())
				.collect(Collectors.toList());
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
	public Map<String, List<Posting>> getPostingsMap(String token, int startOffset, boolean prefixSearch) throws IOException {
		Map<String, List<Posting>> postings = new HashMap<String, List<Posting>>();
		this.indexFile.seek(startOffset);
		while(true) {
			try {
				String readToken = Token.read(this.indexFile);
				int postingsLength = this.indexFile.readInt();
				
				if(prefixSearch) {
					if(readToken.startsWith(token)) {
						List<Posting> readPostings = this.readPostings(postingsLength);
						if(!postings.containsKey(readToken)) {
							postings.put(readToken, readPostings);
						}
						else {
							postings.get(readToken).addAll(readPostings);
						}
						continue;
					}
					else if(readToken.compareTo(token) > 0){
						break;
					}
				}			
				else if(readToken.equals(token)) {
					postings.put(readToken, this.readPostings(postingsLength));
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