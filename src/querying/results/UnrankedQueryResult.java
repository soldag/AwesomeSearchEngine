package querying.results;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import postings.PostingTable;

public class UnrankedQueryResult implements QueryResult {
	
	/**
	 * Contains the found postings per token.
	 */
	private final PostingTable tokenPostings;
	
	/**
	 * Contains a list of documents that match a LinkTo-query.
	 */
	private final Set<Integer> linkingDocuments;
	
	/**
	 * Contains the map of spelling corrections. Key is the original token, value the corrected one.
	 */
	private final Map<String, String> spellingCorrections;
	
	/**
	 * Contains the token frequencies in the whole corpus.
	 */
	private final TObjectIntMap<String> collectionFrequencies;
	
	/**
	 * Contains the corresponding original result (i.e. when prf is enabled), if present.
	 */
	private final QueryResult originalQueryResult;

	
	/**
	 * Creates a new, empty QueryResult instance.
	 */
	public UnrankedQueryResult() {
		this(new PostingTable(), new TObjectIntHashMap<String>());
	}
	
	/**
	 * Creates a new QueryResult instance exclusively for token-queries.
	 * @param tokenPostings
	 * @param collectionFrequencies
	 */
	public UnrankedQueryResult(PostingTable tokenPostings, TObjectIntMap<String> collectionFrequencies) {
		this(tokenPostings, new HashMap<String, String>(), collectionFrequencies);
	}
	
	/**
	 * Creates a new QueryResult instance exclusively for token-queries.
	 * @param tokenPostings
	 * @param spellingCorrections
	 * @param collectionFrequencies
	 */
	public UnrankedQueryResult(PostingTable tokenPostings, Map<String, String> spellingCorrections, TObjectIntMap<String> collectionFrequencies) {
		this(tokenPostings, new HashSet<Integer>(), spellingCorrections, collectionFrequencies);
	}
	
	/**
	 * Creates a new QueryResult instance exclusively for ForIn-queries..
	 * @param linkingDocuments
	 */
	public UnrankedQueryResult(Set<Integer> linkingDocuments) {
		this(new PostingTable(), linkingDocuments, new HashMap<String, String>(), new TObjectIntHashMap<String>());
	}
	
	/**
	 * Creates a new QueryResult instance.
	 * @param postingTable
	 * @param linkingDocuments
	 * @param spellingCorrections
	 * @param collectionFrequencies
	 */
	public UnrankedQueryResult(PostingTable postingTable, Set<Integer> linkingDocuments, Map<String, String> spellingCorrections, TObjectIntMap<String> collectionFrequencies) {
		this(postingTable, linkingDocuments, spellingCorrections, collectionFrequencies, null);
	}
	
	/**
	 * Creates a new QueryResult instance with original result.
	 * @param postingTable
	 * @param linkingDocuments
	 * @param spellingCorrections
	 * @param collectionFrequencies
	 * @param originalQueryResult
	 */
	public UnrankedQueryResult(PostingTable postingTable, Set<Integer> linkingDocuments, 
			Map<String, String> spellingCorrections, TObjectIntMap<String> collectionFrequencies, 
			QueryResult originalQueryResult) {
		this.tokenPostings = postingTable;
		this.linkingDocuments = linkingDocuments;
		this.collectionFrequencies = collectionFrequencies;
		this.originalQueryResult = originalQueryResult;
		
		// Assure, that spelling corrections only include tokens that are part of the posting table
		Set<String> existingTokens = this.tokenPostings.tokenSet();
		this.spellingCorrections =  spellingCorrections.entrySet().stream()
				.filter(x -> existingTokens.contains(x.getKey()))
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
	}
	
	/**
	 * Creates a new UnrankedQueryResult instance based on the given results.
	 * @param result
	 * @param originalResult
	 * @return
	 */
	public static UnrankedQueryResult fromResults(UnrankedQueryResult result, QueryResult originalResult) {
		return new UnrankedQueryResult(result.getPostings(), result.getLinkingDocuments(), 
				result.getSpellingCorrections(), result.getCollectionFrequencies(), originalResult);
	}
	
	
	@Override
	public PostingTable getPostings() {
		return tokenPostings;
	}

	@Override
	public Set<Integer> getLinkingDocuments() {
		return this.linkingDocuments;
	}

	@Override
	public Map<String, String> getSpellingCorrections() {
		return spellingCorrections;
	}

	/**
	 * Gets the token frequencies in the whole corpus.
	 * @return
	 */
	public TObjectIntMap<String> getCollectionFrequencies() {
		return this.collectionFrequencies;
	}
	
	/**
     * Determines, whether the current result has a corresponding original result (i.e. when prf is enabled).
   	 * @return
	 */
	public boolean hasOriginalResult() {
		return this.originalQueryResult != null;
	}

	/**
     * Gets the corresponding original result (i.e. when prf is enabled).
   	 * @return
	 */
	public QueryResult getOriginalResult() {
		return this.originalQueryResult;
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
		Set<Integer> linkingDocuments = new HashSet<Integer>();
		for(UnrankedQueryResult result: results) {
			linkingDocuments.addAll(result.getLinkingDocuments());
		}
		
		
		return new UnrankedQueryResult(disjunctedTokenPostings, linkingDocuments, 
				disjunctSpellingCorrections(results), disjunctCollectionFrequencies(results));
	}
	
	/**
	 * Conjuncts multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	public static UnrankedQueryResult conjunct(UnrankedQueryResult...results) {
		// Get intersection of document ids
		Set<Integer> documentIds = intersectDocumentIds(Arrays.asList(
										intersectDocumentIds(Arrays.stream(results).map(result -> result.getPostings().documentIdSet()).collect(Collectors.toList())),
										intersectDocumentIds(Arrays.stream(results).map(result -> result.getLinkingDocuments()).collect(Collectors.toList()))));
		
		// Conjunct postings
		PostingTable[] postingTables = Arrays.stream(results).map(x -> x.getPostings()).toArray(PostingTable[]::new);
		PostingTable conjunctedTokenPostings = PostingTable.disjunctRetained(documentIds, postingTables);
		
		// Conjunct linked documents
		Set<Integer> linkingDocuments = new HashSet<Integer>();
		for(UnrankedQueryResult result: results) {
			linkingDocuments.addAll(result.getLinkingDocuments());
		}
		linkingDocuments.retainAll(documentIds);
		
		return new UnrankedQueryResult(conjunctedTokenPostings, linkingDocuments, 
				disjunctSpellingCorrections(results), disjunctCollectionFrequencies(results));
	}
	
	/**
	 * Calculates the relative complement (set-theoretic difference) of multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	public static UnrankedQueryResult relativeComplement(UnrankedQueryResult...results) {
		// Relative complement of document ids
		Set<Integer> documentIds = new HashSet<Integer>(Sets.union(results[0].getPostings().documentIdSet(), results[0].getLinkingDocuments()));
		for(int i = 1; i < results.length; i++) {
			documentIds.removeAll(results[i].getPostings().documentIdSet());
			documentIds.removeAll(results[i].getLinkingDocuments());
		}
		
		// Relative complement of postings
		PostingTable[] postingTables = Arrays.stream(results).map(x -> x.getPostings()).toArray(PostingTable[]::new);
		PostingTable conjunctedTokenPostings = PostingTable.disjunctRetained(documentIds, postingTables);
		
		// Relative complement of linked documents
		Set<Integer> linkingDocuments = new HashSet<Integer>();
		for(UnrankedQueryResult result: results) {
			linkingDocuments.addAll(result.getLinkingDocuments());
		}
		linkingDocuments.retainAll(documentIds);		
		
		return new UnrankedQueryResult(conjunctedTokenPostings, linkingDocuments, 
				disjunctSpellingCorrections(results), disjunctCollectionFrequencies(results));
	}
	
	/**
	 * Intersects the given sets of document ids. Empty sets are ignored.
	 * @param documentIds
	 * @return
	 */
	private static Set<Integer> intersectDocumentIds(List<Set<Integer>> documentIds) {
		Set<Integer> intersection = documentIds.get(0);
		for(Set<Integer> set: documentIds) {
			if(intersection.isEmpty()) {
				intersection = set;
			}
			else if(!set.isEmpty()) {
				intersection.retainAll(set);
			}
		}
		
		return intersection;
	}
	
	/**
	 * Disjuncts the spelling corrections of multiple UnrankedQueryResult instances.
	 * @param results
	 * @return
	 */
	private static Map<String, String> disjunctSpellingCorrections(UnrankedQueryResult...results) {
		return Arrays.stream(results)
				.flatMap(result -> result.getSpellingCorrections().entrySet().stream())
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
	}
	
	/**
	 * Disjuncts the collection frequencies of multiple UnrankedQueryResult instances.
	 * @param results
	 * @return
	 */
	private static TObjectIntMap<String> disjunctCollectionFrequencies(UnrankedQueryResult...results) {
		TObjectIntMap<String> collectionFrequencies = new TObjectIntHashMap<String>();
		Arrays.stream(results)
				.map(result -> result.getCollectionFrequencies())
				.forEach(map -> collectionFrequencies.putAll(map));
		
		return collectionFrequencies;
	}
}
