package querying.queries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeywordQuery extends PrfQuery {
	
	/**
	 * Contains the type of the query.
	 */
	public static final String TYPE = "KEYWORD";
	
	/**
	 * Contains the list of query tokens.
	 */
	private final List<String> queryTokens;
	
	
	/**
	 * Creates a new KeywordQuery instance with prf turned off.
	 * @param queryTokens
	 */
	public KeywordQuery(List<String> queryTokens) {
		this.queryTokens = queryTokens;
	}
	
	/**
	 * Creates a new KeywordQuery instance with prf turned on.
	 * @param queryTokens
	 * @param prf
	 */
	public KeywordQuery(List<String> queryTokens, int prf) {
		super(prf);
		this.queryTokens = queryTokens;
	}
	
	
	/**
	 * Gets the list of query tokens.
	 * @return
	 */
	public List<String> getQueryTokens() {
		return this.queryTokens;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public boolean containsToken(String token) {
		return this.getQueryTokens().contains(token);
	}

	@Override
	public PrfQuery extendBy(List<String> tokens) {
		Set<String> newTokens = new HashSet<String>(this.getQueryTokens());
		newTokens.addAll(tokens);
		
		return new KeywordQuery(new ArrayList<String>(newTokens));
	}
	
	@Override
	public String toString() {
		return String.join(" ", this.getQueryTokens());
	}
}
