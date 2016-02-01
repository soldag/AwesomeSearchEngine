package querying.results;

import java.util.Map;
import java.util.Set;

import postings.PostingTable;

public interface QueryResult {

	/**
	 * Gets the matching postings.
	 * @return
	 */
	public PostingTable getPostings();
	
	/**
	 * Gets a list of documents that match a LinkTo-query.
	 * @return
	 */
	public Set<Integer> getLinkingDocuments();

	/**
	 * Gets the map of spelling corrections. Key is the original token, value the corrected one.
	 * @return
	 */
	public Map<String, String> getSpellingCorrections();
	
	
	/**
	 * Determines, whether the current result has a corresponding original result (i.e. when prf is enabled).
	 * @return
	 */
	public boolean hasOriginalResult();
	
	/**
	 * Gets the corresponding original result (i.e. when prf is enabled).
	 * @return
	 */
	public QueryResult getOriginalResult();
}
