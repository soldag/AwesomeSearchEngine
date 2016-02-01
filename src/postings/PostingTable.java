package postings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import postings.positions.EagerPositionMap;
import postings.positions.PositionMap;

public class PostingTable {
	
	private final Table<String, Integer, PositionMap> table;
	
	
	/**
	 * Creates a new, empty PostingTable instance.
	 */
	public PostingTable() {
		this.table = HashBasedTable.<String, Integer, PositionMap>create();
	}
	
	
	/**
	 * Gets the set of all tokens contained in this table.
	 * @return
	 */
	public Set<String> tokenSet() {
		return this.table.rowKeySet();
	}
	
	/**
	 * Gets the set of all document ids contained in this table.
	 * @return
	 */
	public Set<Integer> documentIdSet() {
		return this.table.columnKeySet();
	}
	
	
	/**
	 * Returns all postings of a specified token.
	 * @param token
	 * @return
	 */
	public TokenPostings ofToken(String token) {
		return new TokenPostings(this.table.row(token));
	}
	
	/**
	 * Returns all postings of a specified document.
	 * @param documentId
	 * @return
	 */
	public DocumentPostings ofDocument(int documentId) {
		return new DocumentPostings(this.table.column(documentId));
	}
	
	
	/**
	 * Determines, whether there is at least one posting for the given token.
	 * @param token
	 * @return
	 */
	public boolean containsToken(String token) {
		return this.table.containsRow(token);
	}
	
	/**
	 * Determines, whether there is at least one posting of the given document.
	 * @param documentId
	 * @return
	 */
	public boolean containsDocument(int documentId) {
		return this.table.containsColumn(documentId);
	}
	
	/**
	 * Determines, whether there is at least one posting of the given document for the specified token.
	 * @param token
	 * @param documentId
	 * @return
	 */
	public boolean contains(String token, int documentId) {
		return this.table.contains(token, documentId);
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
			this.table.get(token, documentId).putAll(positions);
		}
		else {
			this.table.put(token, documentId, positions);
		}
	}
	
	/**
	 * Adds postings for a specific token.
	 * @param token
	 * @param postings
	 */
	public void putAll(String token, TokenPostings postings) {
		for(Map.Entry<Integer, PositionMap> entry: postings.entrySet()) {
			int documentId = entry.getKey();
			PositionMap positions = entry.getValue();
			this.table.put(token, documentId, positions);
		}
	}
	
	/**
	 * Adds postings for a specific document.
	 * @param documentId
	 * @param postings
	 */
	public void putAll(int documentId, DocumentPostings postings) {
		for(Map.Entry<String, PositionMap> entry: postings.entrySet()) {
			String token = entry.getKey();
			PositionMap positions = entry.getValue();
			this.table.put(token, documentId, positions);
		}
	}
	
	/**
	 * Merges the given PostingTable instance into the current one.
	 * @param postingTable
	 */
	public void putAll(PostingTable postingTable) {
		this.table.putAll(postingTable.table);
	}
	
	
	/**
	 * Removes all postings associated with the given token.
	 * @param token
	 */
	public void remove(String token) {
		this.table.row(token).clear();
	}
	
	/**
	 * Removes all postings associated with the given document.
	 * @param documentId
	 */
	public void remove(int documentId) {
		this.table.column(documentId).clear();
	}
	
	/**
	 * Removes all postings associated with the given token and document.
	 * @param token
	 * @param documentId
	 */
	public void remove(String token, int documentId) {
		this.table.remove(token, documentId);
	}
	
	
	/**
	 * Determines, whether the posting table is empty.
	 * @return
	 */
	public boolean isEmpty() {
		return this.table.isEmpty();
	}
	
	/**
	 * Gets the number of postings in this table.
	 * @return
	 */
	public int size() {
		return this.table.size();
	}
	
	/**
	 * Gets the number of occurrences of all tokens.
	 * @return
	 */
	public int totalTokenOccurencesCount() {
		return this.table.values().stream()
				.mapToInt(positionMap -> positionMap.size())
				.sum();
	}
	
	/**
	 * Deletes all postings from this table.
	 */
	public void clear() {
		this.table.clear();
	}
	
	@Override
	public String toString() {
		return this.table.toString();
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
		// Get intersection of document ids
		Set<Integer> documentIds = new HashSet<Integer>(postingTables[0].documentIdSet());
		for(int i = 1; i < postingTables.length; i++) {
			documentIds.retainAll(postingTables[i].documentIdSet());
		}

		return PostingTable.disjunctRetained(documentIds, postingTables);
	}

	/**
	 * Calculate the relative complement (set-theoretic difference) of multiple PostingTable instances.
	 * @param postingTables
	 * @return
	 */
	public static PostingTable relativeComplement(PostingTable...postingTables) {		
		// Get complement of document ids
		Set<Integer> documentIds = new HashSet<Integer>(postingTables[0].documentIdSet());
		for(int i = 1; i < postingTables.length; i++) {
			documentIds.removeAll(postingTables[i].documentIdSet());
		}

		return PostingTable.disjunctRetained(documentIds, postingTables);
	}
	
	/**
	 * Disjuncts the given posting tables and retains only the given set of document ids.
	 * @param documentIds
	 * @param postingTables
	 * @return
	 */
	public static PostingTable disjunctRetained(Set<Integer> documentIds, PostingTable...postingTables) {
		PostingTable result = new PostingTable();
		Arrays.stream(postingTables)
				.flatMap(postingTable -> postingTable.table.cellSet().stream())
				.filter(cell -> documentIds.contains(cell.getColumnKey()))
				.forEach(cell -> result.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));

		return result;
	}
}
