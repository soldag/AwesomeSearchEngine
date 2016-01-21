package postings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import documents.PatentDocument;
import postings.positions.EagerPositionMap;
import postings.positions.PositionMap;

public class PostingTable {
	
	private final Map<String, TokenPostings> tokenPostings;
	
	private final Map<Integer, DocumentPostings> documentPostings;
	
	
	/**
	 * Creates a new, empty PostingTable instance.
	 */
	public PostingTable() {
		this.tokenPostings = new HashMap<String, TokenPostings>();
		this.documentPostings = new LinkedHashMap<Integer, DocumentPostings>();
	}
	
	/**
	 * Creates a new PostingTable instance.
	 * @param postingTable
	 * @param documentPostings
	 */
	public PostingTable(Map<String, TokenPostings> tokenPostings, Map<Integer, DocumentPostings> documentPostings) {
		this.tokenPostings = tokenPostings;
		this.documentPostings = documentPostings;
	}
	
	
	/**
	 * Gets the set of all tokens contained in this table.
	 * @return
	 */
	public Set<String> tokenSet() {
		return this.tokenPostings.keySet();
	}
	
	/**
	 * Gets the set of all document ids contained in this table.
	 * @return
	 */
	public Set<Integer> documentIdSet() {
		return this.documentPostings.keySet();
	}
	
	
	/**
	 * Returns all postings of a specified token.
	 * @param token
	 * @return
	 */
	public TokenPostings ofToken(String token) {
		return this.tokenPostings.get(token);
	}
	
	/**
	 * Returns all postings of a specified document.
	 * @param documentId
	 * @return
	 */
	public DocumentPostings ofDocument(int documentId) {
		return this.documentPostings.get(documentId);
	}
	
	
	/**
	 * Determines, whether there is at least one posting for the given token.
	 * @param token
	 * @return
	 */
	public boolean containsToken(String token) {
		return this.tokenPostings.containsKey(token);
	}
	
	/**
	 * Determines, whether there is at least one posting of the given document.
	 * @param documentId
	 * @return
	 */
	public boolean containsDocument(int documentId) {
		return this.documentPostings.containsKey(documentId);
	}
	
	/**
	 * Determines, whether there is at least one posting of the given document for the specified token.
	 * @param token
	 * @param documentId
	 * @return
	 */
	public boolean contains(String token, int documentId) {
		TokenPostings tokenPostings = this.tokenPostings.get(token);
		if(tokenPostings != null) {
			return tokenPostings.containsDocument(documentId);
		}
		
		return false;
	}
	
	/**
	 * Determines, whether there is at least one posting of the given document for the specified token.
	 * @param token
	 * @param document
	 * @return
	 */
	public boolean contains(String token, PatentDocument document) {
		return this.contains(token, document.getId());
	}

	
	/**
	 * Adds a new posting with a single position.
	 * @param token
	 * @param documentId
	 * @param contentType
	 * @param position
	 */
	public void put(String token, int documentId, ContentType contentType, int position) {
		// Create position map
		PositionMap positions = new EagerPositionMap();
		positions.put(contentType, position);
		this.put(token, documentId, positions);
	}
	
	/**
	 * Adds postings for a specific document.
	 * @param token
	 * @param documentId
	 * @param positions
	 */
	public void put(String token, int documentId, PositionMap positions) {
		if(this.contains(token, documentId)) {
			this.ofToken(token).ofDocument(documentId).putAll(positions);
		}
		else {
			// Create token postings
			Map<Integer, PositionMap> tokenPostings = new HashMap<Integer, PositionMap>();
			tokenPostings.put(documentId, positions);
			
			// Create document postings
			Map<String, PositionMap> documentPostings = new HashMap<String, PositionMap>();
			documentPostings.put(token, positions);
			
			if(this.containsToken(token)) {
				this.documentPostings.put(documentId, new DocumentPostings(documentPostings));
				this.ofToken(token).put(documentId, positions);
			}
			else if(this.containsDocument(documentId)) {
				this.tokenPostings.put(token, new TokenPostings(tokenPostings));
				this.ofDocument(documentId).put(token, positions);
			}
			else {
				this.tokenPostings.put(token, new TokenPostings(tokenPostings));
				this.documentPostings.put(documentId, new DocumentPostings(documentPostings));
			}
		}
	}
	
	/**
	 * Adds postings for a specific token.
	 * @param token
	 * @param postings
	 */
	public void putAll(String token, TokenPostings postings) {
		this.tokenPostings.put(token, postings);
		
		for(Map.Entry<Integer, PositionMap> entry: postings.entrySet()) {
			int documentId = entry.getKey();
			PositionMap positions = entry.getValue();
			if(this.documentPostings.containsKey(documentId)) {
				this.ofDocument(documentId).put(token, positions);
			}
			else {
				Map<String, PositionMap> documentPostings = new HashMap<String, PositionMap>();
				documentPostings.put(token, positions);
				this.documentPostings.put(documentId, new DocumentPostings(documentPostings));
			}
		}
	}
	
	/**
	 * Adds postings for a specific document.
	 * @param documentId
	 * @param postings
	 */
	public void putAll(int documentId, DocumentPostings postings) {
		this.documentPostings.put(documentId, postings);
		
		for(Map.Entry<String, PositionMap> entry: postings.entrySet()) {
			String token = entry.getKey();
			PositionMap positions = entry.getValue();
			if(this.tokenPostings.containsKey(token)) {
				this.ofToken(token).put(documentId, positions);
			}
			else {
				Map<Integer, PositionMap> tokenPostings = new HashMap<Integer, PositionMap>();
				tokenPostings.put(documentId, positions);
				this.tokenPostings.put(token, new TokenPostings(tokenPostings));
			}
		}
	}
	
	/**
	 * Adds postings for a specific document.
	 * @param document
	 * @param postings
	 */
	public void putAll(PatentDocument document, DocumentPostings postings) {
		this.putAll(document.getId(), postings);
	}
	
	/**
	 * Merges the given PostingTable instance into the current one.
	 * @param postingTable
	 */
	public void putAll(PostingTable postingTable) {
		for(Map.Entry<String, TokenPostings> entry: postingTable.tokenPostings.entrySet()) {
			this.putAll(entry.getKey(), entry.getValue());
		}
	}
	
	
	/**
	 * Removes all postings associated with the given token.
	 * @param token
	 */
	public void remove(String token) {
		this.ofToken(token).documentIdSet().stream()
				.forEach(documentId -> this.remove(token, documentId));
	}
	
	/**
	 * Removes all postings associated with the given document.
	 * @param documentId
	 */
	public void remove(int documentId) {
		this.documentPostings.remove(documentId);
		this.tokenPostings.values().stream().forEach(tokenPosting -> tokenPosting.remove(documentId));
	}
	
	/**
	 * Removes all postings associated with the given document.
	 * @param document
	 */
	public void remove(PatentDocument document) {
		this.remove(document.getId());
	}
	
	/**
	 * Removes all postings associated with the given token and document.
	 * @param token
	 * @param documentId
	 */
	public void remove(String token, int documentId) {
		this.ofToken(token).remove(documentId);
		this.ofDocument(documentId).remove(token);
	}
	
	/**
	 * Removes all postings associated with the given token and document.
	 * @param token
	 * @param document
	 */
	public void remove(String token, PatentDocument document) {
		this.remove(token, document.getId());
	}
	
	
	/**
	 * Determines, whether the posting table is empty.
	 * @return
	 */
	public boolean isEmpty() {
		return this.tokenPostings.isEmpty() && this.documentPostings.isEmpty();
	}
	
	/**
	 * Gets the number of postings in this table.
	 * @return
	 */
	public int size() {
		return this.tokenPostings.values().stream()
					.mapToInt(tokenPostings -> tokenPostings.size())
					.sum();
	}
	
	/**
	 * Gets the number of occurrences of all tokens.
	 * @return
	 */
	public int totalTokenOccurencesCount() {
		return this.tokenPostings.values().stream()
					.mapToInt(tokenPostings -> tokenPostings.getTotalOccurencesCount())
					.sum();
	}
	
	/**
	 * Deletes all postings from this table.
	 */
	public void clear() {
		this.tokenPostings.clear();
		this.documentPostings.clear();
	}
	
	@Override
	public String toString() {
		return this.tokenPostings.toString();
	}
	
	
	/**
	 * Disjuncts multiple PostingTable instances.
	 * @param tokenPostings
	 * @return
	 */
	public static PostingTable disjunct(PostingTable...tokenPostings) {
		PostingTable result = new PostingTable();
		for(PostingTable entry: tokenPostings) {
			result.putAll(entry);
		}
		
		return result;
	}
	
	/**
	 * Conjuncts multiple PostingTable instances.
	 * @param postingTables
	 * @return
	 */
	public static PostingTable conjunct(PostingTable...postingTables) {
		PostingTable result = new PostingTable();
		
		// Get intersection of document ids
		Set<Integer> documentIds = new HashSet<Integer>(postingTables[0].documentIdSet());
		for(int i = 1; i < postingTables.length; i++) {
			documentIds.retainAll(postingTables[i].documentIdSet());
		}

		// Add all posting with one of the conjuncted document ids to result
		Arrays.stream(postingTables)
				.flatMap(postingTable -> postingTable.documentPostings.entrySet().stream())
				.filter(entry -> documentIds.contains(entry.getKey()))
				.forEach(entry -> result.putAll(entry.getKey(), entry.getValue()));
		
		return result;
	}

	/**
	 * Calculate the relative complement (set-theoretic difference) of multiple PostingTable instances.
	 * @param postingTables
	 * @return
	 */
	public static PostingTable relativeComplement(PostingTable...postingTables) {
		PostingTable result = new PostingTable();
		
		// Get complement of document ids
		Set<Integer> documentIds = new HashSet<Integer>(postingTables[0].documentIdSet());
		for(int i = 1; i < postingTables.length; i++) {
			documentIds.removeAll(postingTables[i].documentIdSet());
		}

		// Add all posting with one of the filtered document ids to result
		Arrays.stream(postingTables)
				.flatMap(postingTable -> postingTable.documentPostings.entrySet().stream())
				.filter(entry -> documentIds.contains(entry.getKey()))
				.forEach(entry -> result.putAll(entry.getKey(), entry.getValue()));
		
		return result;
	}
}
