package querying.results;

import java.util.Map;

import postings.PostingTable;

public class PrfQueryResult extends UnrankedQueryResult {

	/**
	 * Contains the original result of the query before expanding the search tokens.
	 */
	private UnrankedQueryResult originalQueryResult;
	
	
	/**
	 * Creates a new PrfQueryResult instance.
	 * @param tokenPostings
	 * @param spellingCorrections
	 * @param originalResult
	 */
	public PrfQueryResult(PostingTable tokenPostings, Map<String, String> spellingCorrections, UnrankedQueryResult originalResult) {
		super(tokenPostings, spellingCorrections);
		
		this.originalQueryResult = originalResult;
	}
	
	/**
	 * Creates a new PrfQueryResult instance based on an expanded QueryResult and the original one.
	 * @param prfResult
	 * @param originalResult
	 * @return
	 */
	public static PrfQueryResult fromResults(UnrankedQueryResult prfResult, UnrankedQueryResult originalResult) {
		return new PrfQueryResult(prfResult.getPostings(), prfResult.getSpellingCorrections(), originalResult);
	}
	
	
	/**
	 * Gets the original result of the query expanding the search tokens.
	 * @return
	 */
	public UnrankedQueryResult getOriginalResult() {
		return this.originalQueryResult;
	}
}
