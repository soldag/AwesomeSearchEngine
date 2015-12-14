package postings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import documents.PatentDocument;
import utilities.ImmutableSetCollector;

public class PostingTable {

	/**
	 * Contains the postings mapped to token and document ids.
	 */
	private final Table<String, Integer, PositionMap> postings;
	
	/**
	 * Contains a lookup map for PatentDocument instances.
	 */
	private final Map<Integer, PatentDocument> documents;
	
	/**
	 * Determines, whether the postings table can be edited or not.
	 */
	private final boolean isFinal;
	
	
	/**
	 * Creates a new, empty PostingTable instance.
	 */
	public PostingTable() {
		this.postings = HashBasedTable.create();
		this.documents = new HashMap<Integer, PatentDocument>();
		this.isFinal = false;
	}
	
	/**
	 * Creates a new PostingTable instance.
	 * @param postingTable
	 * @param documents
	 */
	public PostingTable(ImmutableTable<String, Integer, PositionMap> postingTable, Map<Integer, PatentDocument> documents) {
		this.postings = postingTable;
		this.documents = documents;
		this.isFinal = true;
	}
	
	
	/**
	 * Gets the set of all tokens contained in this table.
	 * @return
	 */
	public Set<String> tokenSet() {
		return this.postings.rowKeySet();
	}
	
	/**
	 * Gets the set of all document ids contained in this table.
	 * @return
	 */
	public Set<Integer> documentIdSet() {
		return this.postings.columnKeySet();
	}
	
	/**
	 * Gets the set of all documents contained in this table.
	 * @return
	 */
	public Set<PatentDocument> documentSet() {
		return this.documentIdSet().stream()
				.filter(documentId -> this.documents.containsKey(documentId))
				.map(documentId -> this.documents.get(documentId))
				.collect(ImmutableSetCollector.toImmutableSet());
	}
	
	
	/**
	 * Returns all postings of a specified token.
	 * @param token
	 * @return
	 */
	public TokenPostings ofToken(String token) {
		return new TokenPostings(this.postings.row(token), this.documents);
	}
	
	/**
	 * Returns all postings of a specified document.
	 * @param documentId
	 * @return
	 */
	public DocumentPostings ofDocument(int documentId) {
		return new DocumentPostings(this.postings.column(documentId));
	}
	
	/**
	 * Returns all postings of a specified document.
	 * @param document
	 * @return
	 */
	public DocumentPostings ofDocument(PatentDocument document) {
		return this.ofDocument(document.getId());
	}
	
	
	/**
	 * Determines, whether there is at least one posting for the given token.
	 * @param token
	 * @return
	 */
	public boolean containsToken(String token) {
		return this.postings.containsRow(token);
	}
	
	/**
	 * Determines, whether there is at least one posting of the given document.
	 * @param documentId
	 * @return
	 */
	public boolean containsDocument(int documentId) {
		return this.postings.containsColumn(documentId);
	}
	
	/**
	 * Determines, whether there is at least one posting of the given document.
	 * @param document
	 * @return
	 */
	public boolean containsDocument(PatentDocument document) {
		return this.containsDocument(document.getId());
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
	 * Determines, whether there is at least one posting of the given document for the specified token.
	 * @param token
	 * @param documentId
	 * @return
	 */
	public boolean contains(String token, int documentId) {
		return this.postings.contains(token, documentId);
	}

	
	/**
	 * Adds a new posting with a single position.
	 * @param token
	 * @param documentId
	 * @param contentType
	 * @param position
	 */
	public void put(String token, int documentId, ContentType contentType, int position) {
		if(!this.postings.contains(token, documentId)) {
			this.postings.put(token, documentId, new PositionMap());
		}
		this.postings.get(token, documentId).put(contentType, position);
	}
	
	/**
	 * Adds a new posting with multiple positions.
	 * @param token
	 * @param documentId
	 * @param positions
	 */
	public void put(String token, int documentId, PositionMap positions) {
		if(this.postings.contains(token, documentId)) {
			this.postings.get(token, documentId).putAll(positions);
		}
		else {
			this.postings.put(token, documentId, positions);
		}
	}
	
	/**
	 * Adds a new posting with multiple positions.
	 * @param token
	 * @param documentId
	 * @param contentType
	 * @param positions
	 */
	public void put(String token, int documentId, ContentType contentType, int[] positions) {
		for(int position: positions) {
			this.put(token, documentId, contentType, position);
		}
	}
	
	/**
	 * Adds postings for a specific token.
	 * @param token
	 * @param postings
	 */
	public void putAll(String token, TokenPostings postings) {
		for(Map.Entry<Integer, PositionMap> entry: postings.entrySet()) {
			this.putAll(token, entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Adds postings for a specific document.
	 * @param documentId
	 * @param postings
	 */
	public void putAll(int documentId, DocumentPostings postings) {
		for(Map.Entry<String, PositionMap> entry: postings.entrySet()) {
			this.putAll(entry.getKey(), documentId, entry.getValue());
		}
	}
	
	/**
	 * Adds postings for a specific document.
	 * @param document
	 * @param postings
	 */
	public void putAll(PatentDocument document, DocumentPostings postings) {
		this.putAll(document.getId(), postings);
		this.putDocument(document);
	}
	
	/**
	 * Adds postings for a specific document.
	 * @param token
	 * @param documentId
	 * @param positions
	 */
	public void putAll(String token, int documentId, PositionMap positions) {
		if(!this.postings.contains(token, documentId)) {
			this.postings.put(token, documentId, new PositionMap());
		}
		this.postings.get(token, documentId).putAll(positions);
	}
	
	/**
	 * Merges the given PostingTable instance into the current one.
	 * @param postingTable
	 */
	public void putAll(PostingTable postingTable) {
		this.postings.putAll(postingTable.postings);
		this.documents.putAll(postingTable.documents);
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
		this.postings.column(documentId).keySet().stream()
				.forEach(token -> this.remove(token, documentId));
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
		this.postings.remove(token, documentId);
		this.documents.remove(documentId);
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
		return this.postings.isEmpty();
	}
	
	/**
	 * Gets the number of postings in this table.
	 * @return
	 */
	public int size() {
		return this.postings.size();
	}
	
	/**
	 * Deletes all postings from this table.
	 */
	public void clear() {
		this.postings.clear();
	}
	
	/**
	 * Determines, whether the postings table can be edited or not.
	 * @return
	 */
	public boolean isFinal() {
		return this.isFinal;
	}
	
	@Override
	public String toString() {
		return this.postings.toString();
	}
	
	
	/**
	 * Loads all documents contained in the posting table using the given loading function.
	 * @param lookupFunction
	 */
	public void loadDocuments(Function<Integer, PatentDocument> lookupFunction) {
		Set<Integer> documentIds = Sets.difference(this.documentIdSet(), this.documents.keySet());
		for(int documentId: documentIds) {
			PatentDocument document = lookupFunction.apply(documentId);
			this.putDocument(document);
		}
	}
	
	/**
	 * Adds a document to the internal mapping.
	 * @param document
	 */
	private void putDocument(PatentDocument document) {
		if(!this.documents.containsKey(document.getId())) {
			this.documents.put(document.getId(), document);
		}
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
				.flatMap(x -> x.postings.cellSet().stream())
				.filter(cell -> documentIds.contains(cell.getColumnKey()))
				.forEach(cell -> result.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));

		// Conjunct document maps and filter out abandoned ones.
		Arrays.stream(postingTables)
				.flatMap(x -> x.documents.values().stream())
				.filter(x -> result.containsDocument(x))
				.forEach(x -> result.putDocument(x));
		
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
			.flatMap(x -> x.postings.cellSet().stream())
			.filter(cell -> documentIds.contains(cell.getColumnKey()))
			.forEach(cell -> result.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
		
		// Conjunct document maps and filter out abandoned ones.
		Arrays.stream(postingTables)
				.flatMap(x -> x.documents.values().stream())
				.filter(x -> result.containsDocument(x))
				.forEach(x -> result.putDocument(x));
		
		return result;
	}
}
