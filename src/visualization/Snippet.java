package visualization;

import java.util.HashSet;
import java.util.Set;

public class Snippet {
	
	/**
	 * Contains the text of the snippet.
	 */
	private String snippet;
	
	/**
	 * Contains those tokens, that are part of the query.
	 */
	private Set<String> queryTokens;


	/**
	 * Creates a new Snippet instance.
	 */
	public Snippet() {
		this(null, new HashSet<String>());
	}
	
	/**
	 * Creates a new Snippet instance.
	 * @param snippet
	 */
	public Snippet(String snippet) {
		this(snippet, new HashSet<String>());
	}
	
	/**
	 * Creates a new Snippet instance.
	 * @param snippet
	 * @param queryTokens
	 */
	public Snippet(String snippet, Set<String> queryTokens) {
		this.snippet = snippet;
		this.queryTokens = queryTokens;
	}
	
	@Override
	public String toString() {
		return this.snippet;
	}
	
	/**
	 * Returns the snippet as formatted string, in which query terms are highlighted.
	 * @return
	 */
	public String toFormattedString() {
		String formattedSnippet = this.snippet;
		for(String queryToken: this.queryTokens) {
			formattedSnippet = formattedSnippet.replaceAll("\\b" + queryToken + "\\b", ResultStyle.ANSI_COLOR_GREEN + queryToken + ResultStyle.ANSI_COLOR_RESET);
		}

		return formattedSnippet;
	}
}
