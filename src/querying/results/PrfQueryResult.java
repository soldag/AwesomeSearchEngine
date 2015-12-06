package querying.results;

import java.util.Map;

import postings.PostingTable;

public class PrfQueryResult extends QueryResult {

	/**
	 * Contains the original result of the query before expanding the search tokens.
	 */
	private QueryResult originalQueryResult;
	
	
	/**
	 * Creates a new PrfQueryResult instance.
	 * @param tokenPostings
	 * @param spellingCorrections
	 * @param originalResult
	 */
	public PrfQueryResult(PostingTable tokenPostings, Map<String, String> spellingCorrections, QueryResult originalResult) {
		super(tokenPostings, spellingCorrections);
		
		this.originalQueryResult = originalResult;
	}
	
	/**
	 * Creates a new PrfQueryResult instance based on an expanded QueryResult and the original one.
	 * @param prfResult
	 * @param originalResult
	 * @return
	 */
	public static PrfQueryResult fromResults(QueryResult prfResult, QueryResult originalResult) {
		return new PrfQueryResult(prfResult.getPostings(), prfResult.getSpellingCorrections(), originalResult);
	}
	
	
	/**
	 * Gets the original result of the query expanding the search tokens.
	 * @return
	 */
	public QueryResult getOriginalResult() {
		return this.originalQueryResult;
	}
}
