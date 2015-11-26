package indexing.invertedindex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import indexing.Posting;
import indexing.Token;

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
	private Map<String, List<Posting>> invertedIndex = new HashMap<String, List<Posting>>();
	
	/**
	 * Contains the number of all tokens occurrences in the index.
	 */
	private int totalTokensCount = 0;
	
	
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
	 * @param token
	 * @param posting
	 */
	public void add(String token, Posting posting) {
		if(!this.invertedIndex.containsKey(token)) {
			// Index does not contain token, yet, so create a new posting list containing only the given posting and add it to the index.
			List<Posting> postingList = new ArrayList<Posting>();
			postingList.add(posting);
			this.invertedIndex.put(token, postingList);
		}
		else {
			// Index contains already postings for the token, so adjust corresponding posting list.
			int documentId = posting.getDocumentId();
			List<Posting> postingsList = this.invertedIndex.get(token);
			Optional<Posting> existingPosting = postingsList.stream().filter(x -> x.getDocumentId() == documentId).findFirst();
			if(existingPosting.isPresent()) {
				// Index already contains the posting's document, so merge posting lists and overwrite old posting list with merged one.
				int[] mergedPositions = IntStream
						.concat(
							Arrays.stream(existingPosting.get().getPositions()), 
							Arrays.stream(posting.getPositions())
						)
						.toArray();
				postingsList.set(postingsList.indexOf(existingPosting.get()), new Posting(posting.getDocumentId(), mergedPositions));
			}
			else {
				// Index does not contain the posting's document, so just add posting to posting list.
				this.invertedIndex.get(token).add(posting);
			}
		}
		
		this.totalTokensCount++;
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
		String[] sortedTokens = this.invertedIndex.keySet().stream().sorted().toArray(String[]::new);		
		
		// Open temporary file
		try (RandomAccessFile indexWriter = new RandomAccessFile(indexFile, "rw")) {
			// Write term counts
			indexWriter.writeInt(this.totalTokensCount);
			
			for(String token: sortedTokens) {
				List<Posting> postings = this.invertedIndex.get(token).stream()
											.sorted()
											.collect(Collectors.toList());
				
				// Add seek list entry
				if(createSeekList) {
					this.seekList.put(token, (int)indexWriter.getFilePointer());
				}
				
				// Write token
				Token.write(token, indexWriter);
				
				// Encode postings
				int lastDocumentId = 0;
				ByteArrayOutputStream postingsStream = new ByteArrayOutputStream();
				for(Posting posting: postings) {
					postingsStream.write(posting.toBytes(this.compress, lastDocumentId));
					lastDocumentId = posting.getDocumentId();
				}
				
				// Write postings length and postings
				byte[] postingsBytes = postingsStream.toByteArray();
				indexWriter.writeInt(postingsBytes.length);
				indexWriter.write(postingsBytes);
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
		this.totalTokensCount = 0;
		this.invertedIndex.clear();
		this.seekList.clear();
	}
}
