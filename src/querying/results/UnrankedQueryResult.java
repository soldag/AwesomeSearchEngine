package querying.results;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import postings.PostingTable;

public class UnrankedQueryResult {
	
	/**
	 * Contains the found postings per token.
	 */
	protected final PostingTable tokenPostings;
	
	/**
	 * Contains the map of spelling corrections. Key is the original token, value the corrected one.
	 */
	protected final Map<String, String> spellingCorrections;

	
	/**
	 * Creates a new QueryResult instance.
	 */
	public UnrankedQueryResult() {
		this(new PostingTable());
	}
	
	/**
	 * Creates a new QueryResult instance.
	 * @param tokenPostings
	 */
	public UnrankedQueryResult(PostingTable tokenPostings) {
		this(tokenPostings, new HashMap<String, String>());
	}
	
	/**
	 * Creates a new QueryResult instance.
	 * @param tokenPostings
	 * @param spellingCorrections
	 */
	public UnrankedQueryResult(PostingTable tokenPostings, Map<String, String> spellingCorrections) {
		this.tokenPostings = tokenPostings;
		
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
		
		// Disjunct spelling correction
		Map<String, String> spellingCorrections = disjunctSpellingCorrections(results);
		
		return new UnrankedQueryResult(disjunctedTokenPostings, spellingCorrections);
	}
	
	/**
	 * Conjuncts multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	public static UnrankedQueryResult conjunct(UnrankedQueryResult...results) {
		PostingTable[] tokenPostings = Arrays.stream(results)
											.map(x -> x.getPostings())
											.toArray(PostingTable[]::new);
		PostingTable disjunctedTokenPostings = PostingTable.conjunct(tokenPostings);
		
		return new UnrankedQueryResult(disjunctedTokenPostings, disjunctSpellingCorrections(results));
	}
	
	/**
	 * Calculates the relative complement (set-theoretic difference) of multiple QueryResult instances.
	 * @param results
	 * @return
	 */
	public static UnrankedQueryResult relativeComplement(UnrankedQueryResult...results) {
		PostingTable[] tokenPostings = Arrays.stream(results).map(x -> x.getPostings()).toArray(PostingTable[]::new);
		PostingTable conjunctedTokenPostings = PostingTable.relativeComplement(tokenPostings);
		
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
