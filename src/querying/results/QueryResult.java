package querying.results;

import java.util.Map;

import com.google.common.collect.ImmutableTable;

import parsing.PatentDocument;

public class QueryResult extends GenericQueryResult<PatentDocument, ImmutableTable<PatentDocument, String, Integer[]>> {
	
	/**
	 * Creates new new QueryResult instance.
	 */
	public QueryResult() {
		this(ImmutableTable.<PatentDocument, String, Integer[]>of());
	}
	
	/**
	 * Creates new new QueryResult instance.
	 * @param rowMap
	 */
	public QueryResult(Map<PatentDocument, Map<String, Integer[]>> rowMap) {
		this(QueryResult.rowMapToTable(rowMap));
	}
	
	/**
	 * Creates new new QueryResult instance.
	 * @param postingsTable
	 */
	public QueryResult(ImmutableTable<PatentDocument, String, Integer[]> postingsTable) {
		super(postingsTable);
	}
	
	/**
	 * Creates new new QueryResult instance.
	 * @param rowMap
	 * @param spellingCorrections
	 */
	public QueryResult(Map<PatentDocument, Map<String, Integer[]>> rowMap, Map<String, String> spellingCorrections) {
		this(QueryResult.rowMapToTable(rowMap), spellingCorrections);
	}
	
	/**
	 * Creates new new QueryResult instance.
	 * @param postingsTable
	 * @param spellingCorrections
	 */
	public QueryResult(ImmutableTable<PatentDocument, String, Integer[]> postingsTable, Map<String, String> spellingCorrections) {
		super(postingsTable, spellingCorrections);
	}
	
	
	/**
	 * Constructs a posting table from a given row map.
	 * @param rowMap
	 * @return
	 */
	private static ImmutableTable<PatentDocument, String, Integer[]> rowMapToTable(Map<PatentDocument, Map<String, Integer[]>> rowMap) {
		ImmutableTable.Builder<PatentDocument, String, Integer[]> builder = new ImmutableTable.Builder<PatentDocument, String, Integer[]>();
		rowMap.entrySet().stream()
			.forEach(row -> row.getValue().entrySet().stream()
					.forEach(column -> builder.put(row.getKey(), column.getKey(), column.getValue())));
		
		return builder.build();
	}
}
