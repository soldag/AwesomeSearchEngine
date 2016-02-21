package postings;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import documents.PatentDocument;
import io.index.IndexReader;
import io.index.IndexWriter;
import postings.positions.EagerPositionMap;
import postings.positions.LazyPositionMap;
import postings.positions.PositionMap;

public class TokenPostings {
	
	/**
	 * Contains the number of occurrences of the token in the whole collection.
	 */
	private int totalOccurencesCount = 0;

	/**
	 * Contains the postings for a specific token.
	 */
	private final Map<Integer, PositionMap> postings;
	
	
	/**
	 * Creates a new TokenPostings instance.
	 * @param postings
	 */
	public TokenPostings(Map<Integer, PositionMap> postings) {		
		this(postings, countTotalOccurrences(postings.values()));
	}
	
	/**
	 * Creates a new TokenPostings instance.
	 * @param postings
	 * @param totalOccurrencesCount
	 */
	public TokenPostings(Map<Integer, PositionMap> postings, int totalOccurrencesCount) {
		this.postings = postings;
		this.totalOccurencesCount = totalOccurrencesCount;
	}
	
	
	/**
	 * Counts the number of total occurrences of a collection of position maps.
	 * @param positionMaps
	 * @return
	 */
	private static int countTotalOccurrences(Collection<PositionMap> positionMaps) {
		return positionMaps.stream()
				.mapToInt(positionMap -> positionMap.size())
				.sum();
	}
	
	
	/**
	 * Gets the set of all document ids contained in the postings.
	 * @return
	 */
	public Set<Integer> documentIdSet() {
		return this.postings.keySet();
	}
	
	/**
	 * Returns all positions of the postings.
	 * @return
	 */
	public Collection<PositionMap> positions() {
		return this.postings.values();
	}
	
	/**
	 * Returns all document ids with their corresponding positions.
	 * @return
	 */
	public Set<Map.Entry<Integer, PositionMap>> entrySet() {
		return this.postings.entrySet();
	}
	
	
	public boolean containsDocument(int documentId) {
		return this.postings.containsKey(documentId);
	}
	
	public boolean containsDocument(PatentDocument document) {
		return this.containsDocument(document.getId());
	}
	
	
	/**
	 * Gets the positions for a specific document.
	 * @param documentId
	 * @return
	 */
	public PositionMap ofDocument(int documentId) {
		return this.postings.get(documentId);
	}
	
	/**
	 * Gets the positions for a specific document.
	 * @param document
	 * @return
	 */
	public PositionMap ofDocument(PatentDocument document) {
		return this.postings.get(document.getId());
	}
	
	
	/**
	 * Adds a new posting for the token.
	 * @param documentId
	 * @param positions
	 */
	public void put(int documentId, PositionMap positions) {
		if(this.postings.containsKey(documentId)) {
			this.postings.get(documentId).putAll(positions);
		}
		else {
			this.postings.put(documentId, positions);
		}
	}

	/**
	 * Adds a new posting for the token.
	 * @param document
	 * @param positions
	 */
	public void put(PatentDocument document, PositionMap positions) {
		this.put(document.getId(), positions);
	}
	
	/**
	 * Adds all given postings for the token.
	 * @param postings
	 */
	public void putAll(TokenPostings postings) {
		this.postings.putAll(postings.postings);
	}
	
	
	/**
	 * Remove postings of the given document.
	 * @param documentId
	 */
	public void remove(int documentId) {
		this.postings.remove(documentId);
	}
	
	/**
	 * Remove postings of the given document.
	 * @param document
	 */
	public void remove(PatentDocument document) {
		this.remove(document.getId());
	}
	
	
	/**
	 * Gets the number of all occurrences of the tokens.
	 * @return
	 */
	public int getTotalOccurencesCount() {
		return this.totalOccurencesCount;
	}
	
	/**
	 * Gets the number of documents mapped to the current token.
	 * @return
	 */
	public int size() {
		return this.postings.size();
	}
	
	
	@Override
	public String toString() {
		return this.postings.toString();
	}
	
	/**
	 * Loads postings for a specific token from a given file reader. 
	 * @param frequencyIndexReader
	 * @param positionalIndexReader
	 * @param loadPositions
	 * @return
	 * @throws IOException
	 */
	public static TokenPostings load(IndexReader frequencyIndexReader, IndexReader positionalIndexReader, boolean loadPositions) throws IOException {
		// Read total occurrences count
		int totalOccurrencesCount = frequencyIndexReader.readInt();
		
		// Load postings
		int lastDocumentId = 0;
		Map<Integer, PositionMap> postings = new HashMap<Integer, PositionMap>();
		while(frequencyIndexReader.getFilePointer() < frequencyIndexReader.length()) {
			// Read document id
			int documentId = frequencyIndexReader.readInt();
			if(frequencyIndexReader.isCompressed()) {
				documentId += lastDocumentId;
				lastDocumentId = documentId;
			}
			
			// Read positions grouped by content type
			PositionMap positionMap; 
			if(loadPositions) {
				positionMap = EagerPositionMap.load(frequencyIndexReader, positionalIndexReader);
			}
			else {
				positionMap = LazyPositionMap.load(frequencyIndexReader, positionalIndexReader);
			}
			
			postings.put(documentId, positionMap);
		}
		
		return new TokenPostings(postings, totalOccurrencesCount);
	}
	
	/**
	 * Saves the postings using the given file writer.
	 * @param frequencyIndexWriter
	 * @param positionalIndexWriter
	 * @throws IOException
	 */
	public void save(IndexWriter frequencyIndexWriter, IndexWriter positionalIndexWriter) throws IOException {		
		// Write total occurrences count
		frequencyIndexWriter.writeInt(this.getTotalOccurencesCount());
		
		// Write postings
		int lastDocumentId = 0;
		int[] sortedDocumentIds = this.documentIdSet().stream().mapToInt(x -> x.intValue()).sorted().toArray();
		for(int documentId: sortedDocumentIds) {
			PositionMap positionMap = this.ofDocument(documentId);
			
			// Write document id
			if(frequencyIndexWriter.isCompressed()) {
				int originalDocumentId = documentId;
				documentId -= lastDocumentId;
				lastDocumentId = originalDocumentId;
			}
			frequencyIndexWriter.writeInt(documentId);
			
			// Write positions
			if(positionalIndexWriter != null) {
				positionalIndexWriter.startSkippingArea();
				positionMap.save(frequencyIndexWriter, positionalIndexWriter);
				positionalIndexWriter.endSkippingArea();
			}
		}
	}
}
