package querying.queries;

import java.util.List;

public class PhraseQuery extends KeywordQuery {

	/**
	 * Contains the type of the query.
	 */
	public static final String TYPE = "PHRASE";
	
	
	/**
	 * Creates a new PhraseQuery instance with prf turned off.
	 * @param phraseTokens
	 */
	public PhraseQuery(List<String> phraseTokens) {
		this(phraseTokens, 0);
	}
	
	/**
	 * Creates a new PhraseQuery instance with prf turned on.
	 * @param phraseTokens
	 * @param prf
	 */
	public PhraseQuery(List<String> phraseTokens, int prf) {
		super(phraseTokens, prf);
	}

	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public PrfQuery extendBy(List<String> tokens) {
		return new MixedQuery(new PhraseQuery(this.getQueryTokens()), new KeywordQuery(tokens));
	}
	
	@Override
	public String toString() {
		return String.format("\"%s\"", super.toString());
	}
}
