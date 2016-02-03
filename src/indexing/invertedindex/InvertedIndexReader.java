package indexing.invertedindex;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
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
	 * Contains the corresponding seek list.
	 */
	private InvertedIndexSeekList seekList;
	
	/**
	 * Contains the number of all tokens occurrences in the index.
	 */
	private int totalTokenCount = 0;
	
	
	/**
	 * Creates a new InvertedIndexReader instance.
	 * @param indexFile
	 * @param seekListFile
	 * @param isCompressed
	 * @throws IOException
	 */
	public InvertedIndexReader(File indexFile, File seekListFile, boolean isCompressed) throws IOException {
		this.indexFile = FileReaderWriterFactory.getInstance().getDirectIndexReader(indexFile, isCompressed);
		this.totalTokenCount = this.indexFile.readInt();
		
		this.seekList = new InvertedIndexSeekList();
		this.seekList.load(FileReaderWriterFactory.getInstance().getDirectIndexReader(seekListFile, isCompressed));
	}
	
	
	/**
	 * Gets the number of all tokens occurrences in the index.
	 * @return
	 */
	public int getTotalTokenCount() {
		return this.totalTokenCount;
	}
	
	
	/**
	 * Gets a map of tokens of the index starting the the given prefix and their number of total occurrences in the corpus.
	 * @param prefix
	 * @return
	 * @throws IOException
	 */
	public TObjectIntMap<String> getTokens(String prefix) throws IOException {
		TObjectIntMap<String> tokens = new TObjectIntHashMap<String>();		
		this.indexFile.seek(this.seekList.get(prefix));
		while(true) {
			try {
				String token = this.indexFile.readString();
				if(token.startsWith(prefix)) {
					int occurrencesCount = this.indexFile.readInt();
					tokens.put(token, occurrencesCount);
				}				
				else if(token.compareTo(prefix) > 0){
					break;
				}
				
				this.indexFile.skipSkippingArea();
			}
			catch(EOFException e) {
				break;
			}
		}
		
		return tokens;
	}
	
	
	/**
	 * Gets a map of postings per token from inverted index.
	 * Additionally, prefix search can be enabled. In this case, all tokens, that start with the given token, are also taken into account.
	 * @param token
	 * @param prefixSearch
	 * @param loadPositions
	 * @return Table of postings
	 * @throws IOException
	 */
	public PostingTable getPostings(String token, boolean prefixSearch, boolean loadPositions) throws IOException {
		long offset = this.seekList.get(token);
		return this.getPostings(token, offset, prefixSearch, loadPositions);
	}

	/**
	 * Gets a map of postings per token from inverted index by specifying a start offset in the index file. 
	 * Additionally, prefix search can be enabled. In this case, all tokens, that start with the given token, are also taken into account.
	 * @param token
	 * @param startOffset
	 * @param prefixSearch
	 * @param loadPositions
	 * @return Table of postings
	 * @throws IOException
	 */
	private PostingTable getPostings(String token, long startOffset, boolean prefixSearch, boolean loadPositions) throws IOException {
		PostingTable postings = new PostingTable();
		
		this.indexFile.seek(startOffset);
		while(true) {
			try {
				String readToken = this.indexFile.readString();
				
				if(prefixSearch) {
					if(readToken.startsWith(token)) {
						TokenPostings readPostings = TokenPostings.load(this.indexFile.getSkippingAreaReader(), loadPositions);
						postings.putAll(readToken, readPostings);
						continue;
					}
				}			
				else if(readToken.equals(token)) {
					TokenPostings readPostings = TokenPostings.load(this.indexFile.getSkippingAreaReader(), loadPositions);
					postings.putAll(readToken, readPostings);
					break;
				}
				
				if(readToken.compareTo(token) > 0){
					break;
				}
				
				this.indexFile.skipSkippingArea();
			}
			catch(EOFException e) {
				break;
			}
		}
		
		return postings;
	}
	

	@Override
	public void close() throws IOException {
		this.indexFile.close();
	}	
}