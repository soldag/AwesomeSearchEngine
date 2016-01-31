package querying.results;

import java.util.Map;

import com.google.common.collect.Multimap;

import postings.PostingTable;

public interface QueryResult {

	/**
	 * Gets the matching postings.
	 * @return
	 */
	public PostingTable getPostings();
	
	/**
	 * Gets a map, that contains for each requested document id a list of ids of documents, which cite the first one. 
	 * @return
	 */
	public Multimap<Integer, Integer> getLinkedDocuments();

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
