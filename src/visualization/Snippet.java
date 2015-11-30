package visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Snippet {
	
	/**
	 * Contains ANSI codes for output formatting.
	 */
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_COLOR_RESET = "\u001B[0m";
	
	/**
	 * Contains the tokens of the snippet.
	 */
	private List<String> tokens;
	
	/**
	 * Contains the indexes of those tokens, that are part of the query.
	 */
	private List<Integer> queryTokenIndexes;

	
	/**
	 * Creates a new Snippet instance.
	 */
	public Snippet() {
		this(new ArrayList<String>(), new ArrayList<Integer>());
	}
	
	/**
	 * Creates a new Snippet instance.
	 * @param tokens
	 * @param queryTokenIndexes
	 */
	public Snippet(List<String> tokens, List<Integer> queryTokenIndexes) {
		this.tokens = tokens;
		this.queryTokenIndexes = queryTokenIndexes;
	}
	
	
	/**
	 * Adds a new token to the snippet.
	 * @param token
	 * @param isQueryToken
	 */
	public void add(String token, boolean isQueryToken) {
		this.tokens.add(token);
		if(isQueryToken) {
			this.queryTokenIndexes.add(this.tokens.size() - 1);
		}
	}
	
	/**
	 * Returns the tokens of the snippet.
	 * @return
	 */
	public List<String> getTokens() {
		return this.tokens;
	}
	
	@Override
	public String toString() {
		return this.tokens.stream().collect(Collectors.joining(" "));
	}
	
	/**
	 * Returns the snippet as formatted string, in which query terms are highlighted.
	 * @return
	 */
	public String toFormattedString() {
		List<String> formattedTokens = new ArrayList<String>(this.tokens.size()); 
		for(int i = 0; i < this.tokens.size(); i++) {
			if(this.queryTokenIndexes.contains(i)) {
				formattedTokens.add(ANSI_GREEN + this.tokens.get(i) + ANSI_COLOR_RESET);
			}
			else {
				formattedTokens.add(this.tokens.get(i));
			}
		}
		
		return formattedTokens.stream().collect(Collectors.joining(" "));
	}
}
