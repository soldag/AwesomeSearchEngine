package querying.results;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Table;

public abstract class GenericQueryResult<R, T extends Table<R, String, Integer[]>> {
	
	/**
	 * Contains the table of postings.
	 */
	protected final T postingsTable;
	
	/**
	 * Contains the map of spelling corrections. Key is the original token, value the corrected one.
	 */
	protected final Map<String, String> spellingCorrections;

	
	/**
	 * Creates a new GenericQueryResult instance.
	 * @param postingsTable
	 */
	public GenericQueryResult(T postingsTable) {
		this(postingsTable, new HashMap<String, String>());
	}
	
	/**
	 * Creates a new GenericQueryResult instance.
	 * @param postingsTable
	 * @param spellingCorrections
	 */
	public GenericQueryResult(T postingsTable, Map<String, String> spellingCorrections) {
		this.postingsTable = postingsTable;
		this.spellingCorrections = spellingCorrections;
	}
	
	
	/**
	 * Gets the table of postings.
	 * @return
	 */
	public T getPostingsTable() {
		return postingsTable;
	}

	/**
	 * Gets the map of spelling corrections. Key is the original token, value the corrected one.
	 * @return
	 */
	public Map<String, String> getSpellingCorrections() {
		return spellingCorrections;
	}
}
