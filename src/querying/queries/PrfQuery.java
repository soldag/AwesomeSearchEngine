package querying.queries;

import java.util.List;

public abstract class PrfQuery implements Query {
	
	/**
	 * Contains the parameter for pseudo-relevance-feedback, that determines, how many documents are taken into account.
	 */
	private final int prf;
	
	
	/**
	 * Initializes the query and turns prf off.
	 */
	public PrfQuery() {
		this(0);
	}
	
	/**
	 * Initializes the query and turns prf on.
	 * @param prf
	 */
	public PrfQuery(int prf) {
		this.prf = prf;
	}
	
	
	/**
	 * Gets the parameter for pseudo-relevance-feedback, that determines, how many documents are taken into account.
	 * @return
	 */
	public int getPrf() {
		return this.prf;
	}
	
	/**
	 * Extends the current query by given tokens. Extended query is returned.
	 * @param tokens
	 * @return
	 */
	public abstract PrfQuery extendBy(List<String> tokens);

	/**
	 * Returns a boolean, that determines, whether the current query contains the given token as query token.
	 * @param token
	 * @return
	 */
	public abstract boolean containsToken(String token);
}
