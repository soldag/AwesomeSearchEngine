package querying.results;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

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
	 * Contains the corresponding original result (i.e. when prf is enabled), if present.
	 */
	private final QueryResult originalQueryResult;

	
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
		this(tokenPostings, new HashSet<Integer>(), spellingCorrections);
	}
	
	/**
	 * Creates a new QueryResult instance exclusively for ForIn-queries..
	 * @param linkingDocuments
	 */
	public UnrankedQueryResult(Set<Integer> linkingDocuments) {
		this(new PostingTable(), linkingDocuments, new HashMap<String, String>());
	}
	
	/**
	 * Creates a new QueryResult instance.
	 * @param postingTable
	 * @param linkingDocuments
	 * @param spellingCorrections
	 */
	public UnrankedQueryResult(PostingTable postingTable, Set<Integer> linkingDocuments, Map<String, String> spellingCorrections) {
		this(postingTable, linkingDocuments, spellingCorrections, null);
	}
	
	/**
	 * Creates a new QueryResult instance with original result.
	 * @param postingTable
	 * @param linkingDocuments
	 * @param spellingCorrections
	 * @param originalQueryResult
	 */
	public UnrankedQueryResult(PostingTable postingTable, Set<Integer> linkingDocuments, 
			Map<String, String> spellingCorrections, QueryResult originalQueryResult) {
		this.tokenPostings = postingTable;
		this.linkingDocuments = linkingDocuments;
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
	public static UnrankedQueryResult fromResults(QueryResult result, QueryResult originalResult) {
		return new UnrankedQueryResult(result.getPostings(), result.getLinkingDocuments(), result.getSpellingCorrections(), originalResult);
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


	@Override
	public boolean hasOriginalResult() {
		return this.originalQueryResult != null;
	}

	@Override
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
		
		
		return new UnrankedQueryResult(disjunctedTokenPostings, linkingDocuments, disjunctSpellingCorrections(results));
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
		
		return new UnrankedQueryResult(conjunctedTokenPostings, linkingDocuments, disjunctSpellingCorrections(results));
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
		
		return new UnrankedQueryResult(conjunctedTokenPostings, linkingDocuments, disjunctSpellingCorrections(results));
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
