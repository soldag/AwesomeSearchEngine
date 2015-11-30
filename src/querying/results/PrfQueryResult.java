package querying.results;

import java.util.Map;

import com.google.common.collect.ImmutableTable;

import parsing.PatentDocument;

public class PrfQueryResult extends QueryResult {

	/**
	 * Contains the original result of the query before expanding the search tokens.
	 */
	private QueryResult originalQueryResult;
	
	
	/**
	 * Creates a new PrfQueryResult instance.
	 * @param postingsTable
	 * @param spellingCorrections
	 * @param originalResult
	 */
	public PrfQueryResult(ImmutableTable<PatentDocument, String, Integer[]> postingsTable, Map<String, String> spellingCorrections, QueryResult originalResult) {
		super(postingsTable, spellingCorrections);
		
		this.originalQueryResult = originalResult;
	}
	
	/**
	 * Creates a new PrfQueryResult instance based on an expanded QueryResult and the original one.
	 * @param prfResult
	 * @param originalResult
	 * @return
	 */
	public static PrfQueryResult fromResults(QueryResult prfResult, QueryResult originalResult) {
		return new PrfQueryResult(prfResult.getPostingsTable(), prfResult.getSpellingCorrections(), originalResult);
	}
	
	
	/**
	 * Gets the original result of the query expanding the search tokens.
	 * @return
	 */
	public QueryResult getOriginalResult() {
		return this.originalQueryResult;
	}
}
