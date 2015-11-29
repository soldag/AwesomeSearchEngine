package querying.results;

import java.util.Map;

import com.google.common.collect.HashBasedTable;

public class IntermediateQueryResult extends GenericQueryResult<Integer, HashBasedTable<Integer, String, Integer[]>> {

	/**
	 * Creates a new IntermediateQueryResult instance.
	 */
	public IntermediateQueryResult() {
		this(HashBasedTable.<Integer, String, Integer[]>create());
	}
	
	/**
	 * Creates a new IntermediateQueryResult instance.
	 * @param postingsTable
	 */
	public IntermediateQueryResult(HashBasedTable<Integer, String, Integer[]> postingsTable) {
		super(postingsTable);
	}
	
	/**
	 * Creates a new IntermediateQueryResult instance.
	 * @param postingsTable
	 * @param spellingCorrections
	 */
	public IntermediateQueryResult(HashBasedTable<Integer, String, Integer[]> postingsTable, Map<String, String> spellingCorrections) {
		super(postingsTable, spellingCorrections);
	}
	
	
	/**
	 * Creates a new IntermediateQueryResult by merging two given IntermediateQueryResult instances.
	 * @param result1
	 * @param result2
	 * @return
	 */
	public static IntermediateQueryResult merge(IntermediateQueryResult result1, IntermediateQueryResult result2) {
		// Merge postings table
		HashBasedTable<Integer, String, Integer[]> postingsTable = result1.getPostingsTable();
		postingsTable.putAll(result2.getPostingsTable());
		
		// Merge spelling corrections
		Map<String, String> spellingCorrections = result1.getSpellingCorrections();
		spellingCorrections.putAll(result2.getSpellingCorrections());
		
		return new IntermediateQueryResult(postingsTable, spellingCorrections);
	}
}
