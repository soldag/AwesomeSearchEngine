package querying.results;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import postings.PostingTable;

public class UnrankedQueryResult {
	
	/**
	 * Contains the found postings per token.
	 */
	protected final PostingTable tokenPostings;
	
	/**
	 * Contains a map, that contains for each requested document id a list of ids of documents, which cite the first one. 
	 */
	protected final Multimap<Integer, Integer> linkedDocuments;
	
	/**
	 * Contains the map of spelling corrections. Key is the original token, value the corrected one.
	 */
	protected final Map<String, String> spellingCorrections;

	
	/**
	 * Creates a new, empty QueryResult instance.
	 */
	public UnrankedQueryResult() {
		this(new PostingTable());
	}
	
	/**
	 * Creates a new QueryResult instance exclusively for token-queries.
	 * @param tokenPostings
	 */
	public UnrankedQueryResult(PostingTable tokenPostings) {
		this(tokenPostings, new HashMap<String, String>());
	}
	
	/**
	 * Creates a new QueryResult instance exclusively for token-queries.
	 * @param tokenPostings
	 * @param spellingCorrections
	 */
	public UnrankedQueryResult(PostingTable tokenPostings, Map<String, String> spellingCorrections) {
		this(tokenPostings, HashMultimap.<Integer, Integer>create(), spellingCorrections);
	}
	
	/**
	 * Creates a new QueryResult instance exclusively for ForIn-queries..
	 * @param linkedDocuments
	 */
	public UnrankedQueryResult(Multimap<Integer, Integer> linkedDocuments) {
		this(new PostingTable(), linkedDocuments, new HashMap<String, String>());
	}
	
	/**
	 * Creates a new QueryResult instance.
	 * @param tokenPostings
	 * @param linkedDocuments
	 * @param spellingCorrections
	 */
	public UnrankedQueryResult(PostingTable tokenPostings, Multimap<Integer, Integer> linkedDocuments, Map<String, String> spellingCorrections) {
		this.tokenPostings = tokenPostings;
		this.linkedDocuments = linkedDocuments;
		
		// Assure, that spelling corrections only include tokens that are part of the posting table
		Set<String> existingTokens = this.tokenPostings.tokenSet();
		this.spellingCorrections =  spellingCorrections.entrySet().stream()
				.filter(x -> existingTokens.contains(x.getKey()))
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
	}
	
	
	/**
	 * Gets the matching postings.
	 * @return
	 */
	public PostingTable getPostings() {
		return tokenPostings;
	}
	
	/**
	 * Gets a map, that contains for each requested document id a list of ids of documents, which cite the first one. 
	 * @return
	 */
	public Multimap<Integer, Integer> getLinkedDocuments() {
		return this.linkedDocuments;
	}

	/**
	 * Gets the map of spelling corrections. Key is the original token, value the corrected one.
	 * @return
	 */
	public Map<String, String> getSpellingCorrections() {
		return spellingCorrections;
	}
	

	/**
	 * Disjuncts multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	public static UnrankedQueryResult disjunct(UnrankedQueryResult...results) {
		// Disjunct postings
		PostingTable[] tokenPostings = Arrays.stream(results)
											.map(x -> x.getPostings())
											.toArray(PostingTable[]::new);
		PostingTable disjunctedTokenPostings = PostingTable.disjunct(tokenPostings);
		
		// Disjunct linked documents
		Multimap<Integer, Integer> linkedDocuments = HashMultimap.<Integer, Integer>create();
		for(UnrankedQueryResult result: results) {
			linkedDocuments.putAll(result.getLinkedDocuments());
		}
		
		return new UnrankedQueryResult(disjunctedTokenPostings, linkedDocuments, disjunctSpellingCorrections(results));
	}
	
	/**
	 * Conjuncts multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	public static UnrankedQueryResult conjunct(UnrankedQueryResult...results) {
		// Conjunct postings
		PostingTable[] tokenPostings = Arrays.stream(results)
											.map(x -> x.getPostings())
											.toArray(PostingTable[]::new);
		PostingTable conjunctedTokenPostings = PostingTable.conjunct(tokenPostings);
		
		// Conjunct linked documents
		Set<Integer> documentIds = new HashSet<Integer>(results[0].getLinkedDocuments().keySet());
		for(int i = 1; i < results.length; i++) {
			documentIds.retainAll(results[i].getLinkedDocuments().keySet());
		}
		Multimap<Integer, Integer> linkedDocuments = HashMultimap.<Integer, Integer>create();
		for(UnrankedQueryResult result: results) {
			for(Map.Entry<Integer, Integer> entry: result.getLinkedDocuments().entries()) {
				if(documentIds.contains(entry.getKey())) {
					linkedDocuments.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		return new UnrankedQueryResult(conjunctedTokenPostings, linkedDocuments, disjunctSpellingCorrections(results));
	}
	
	/**
	 * Calculates the relative complement (set-theoretic difference) of multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	public static UnrankedQueryResult relativeComplement(UnrankedQueryResult...results) {
		// Relative complement of postings
		PostingTable[] tokenPostings = Arrays.stream(results).map(x -> x.getPostings()).toArray(PostingTable[]::new);
		PostingTable conjunctedTokenPostings = PostingTable.relativeComplement(tokenPostings);
		
		// Relative complement of linked documents
		Set<Integer> documentIds = new HashSet<Integer>(results[0].getLinkedDocuments().keySet());
		for(int i = 1; i < results.length; i++) {
			documentIds.removeAll(results[i].getLinkedDocuments().keySet());
		}
		Multimap<Integer, Integer> linkedDocuments = HashMultimap.<Integer, Integer>create();
		for(UnrankedQueryResult result: results) {
			for(Map.Entry<Integer, Integer> entry: result.getLinkedDocuments().entries()) {
				if(documentIds.contains(entry.getKey())) {
					linkedDocuments.put(entry.getKey(), entry.getValue());
				}
			}
		}		
		
		return new UnrankedQueryResult(conjunctedTokenPostings, disjunctSpellingCorrections(results));
	}
	
	/**
	 * Disjuncts the spelling corrections of multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	private static Map<String, String> disjunctSpellingCorrections(UnrankedQueryResult...results) {
		return Arrays.stream(results)
				.flatMap(result -> result.getSpellingCorrections().entrySet().stream())
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
	}
}
