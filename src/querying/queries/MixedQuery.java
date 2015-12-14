package querying.queries;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MixedQuery extends PrfQuery {
	
	/**
	 * Contains the type of the query.
	 */	
	public static final String TYPE = "MIXED";
	
	/**
	 * Contains the subqueries.
	 */
	private final PrfQuery[] queries;
	
	
	/**
	 * Creates a new MixedQuery instance with prf turned off. 
	 * @param queries
	 */
	public MixedQuery(PrfQuery... queries) {
		this.queries = queries;
	}
	
	/**
	 * Creates a new MixedQuery instance with prf turned on.
	 * @param prf
	 * @param queries
	 */
	public MixedQuery(int prf, PrfQuery... queries) {
		super(prf);
		this.queries = queries;
	}
	
	
	/**
	 * Gets the subqueries.
	 * @return
	 */
	public PrfQuery[] getQueries() {
		return this.queries;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public boolean containsToken(String token) {
		return Arrays.stream(this.getQueries()).anyMatch(query -> query.containsToken(token));
	}

	@Override
	public PrfQuery extendBy(List<String> tokens) {
		// Check, whether queries already contains a keyword query
		int queriesCount = this.getQueries().length;
		OptionalInt keywordQueryIndex = IntStream.range(0, queriesCount)
												.filter(index -> this.getQueries()[index].getType() == KeywordQuery.TYPE)
												.findFirst();
		
		PrfQuery[] newQueries;
		if(keywordQueryIndex.isPresent()) {
			// If queries already contains a keyword query, just add extended tokens to it.
			int index = keywordQueryIndex.getAsInt();
			newQueries = Arrays.copyOf(this.getQueries(), queriesCount);
			newQueries[index] = newQueries[index].extendBy(tokens);
		}
		else {
			// If queries does not contain a keyword query, add a new one with the extended tokens.
			newQueries = Arrays.copyOf(this.getQueries(), queriesCount + 1);
			newQueries[queriesCount] = new KeywordQuery(tokens);
		}
		
		return new MixedQuery(newQueries);
	}

	@Override
	public String toString() {
		return Arrays.stream(this.getQueries())
				.map(query -> query.toString())
				.collect(Collectors.joining(" "));
	}
}
