package visualization;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		String snippet = this.snippet;
		for(String queryToken: this.queryTokens) {
			Pattern regex = Pattern.compile("(?i)\\b" + queryToken + "\\b");
			Matcher matcher = regex.matcher(snippet);
			int lastEndIndex = 0;
			StringBuilder formattedSnippetBuilder = new StringBuilder();
			while(matcher.find()) {
				formattedSnippetBuilder.append(snippet.substring(lastEndIndex, matcher.start()));
				formattedSnippetBuilder.append(ResultStyle.ANSI_COLOR_GREEN);
				formattedSnippetBuilder.append(matcher.group());
				formattedSnippetBuilder.append(ResultStyle.ANSI_COLOR_RESET);
				
				lastEndIndex = matcher.end();
			}
			formattedSnippetBuilder.append(snippet.substring(lastEndIndex));
			snippet = formattedSnippetBuilder.toString();
		}

		return snippet;
	}
}
