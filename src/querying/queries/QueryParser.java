package querying.queries;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import textprocessing.TextPreprocessor;

public class QueryParser {

	/**
	 * Contain the group names in used in patterns for matching the queries.
	 */
	private static final String PRF_GROUP = "prf";
	private static final String PHRASE_GROUP = "phrase";
	private static final String BOOL_LEFT_GROUP = "left";
	private static final String BOOL_RIGHT_GROUP = "right";
	private static final String BOOL_OPERATOR_GROUP = "operator";
	private static final String KEYWORDS_GROUP = "keywords";

	/**
	 * Contain patterns of query components, that are used multiple times among the different query patterns.
	 */
	private static final Pattern BOOLEAN_OPERATORS_PATTERN = Pattern.compile("AND|OR|NOT");
	private static final Pattern PHRASE_PATTERN = Pattern.compile("\"(?<" + PHRASE_GROUP + ">.+)\"");
	private static final Pattern PRF_PATTERN = Pattern.compile("(#)(?<" + PRF_GROUP + ">\\d+)");

	/**
	 * Contain patterns for matching queries the different types and identify its components.
	 */
	private static final Pattern PHRASE_QUERY_PATTERN = Pattern.compile(PHRASE_PATTERN + "( " + PRF_PATTERN + ")?");
	private static final Pattern BOOLEAN_QUERY_PATTERN = Pattern.compile("(?<" + BOOL_LEFT_GROUP + ">.+) (?<" + BOOL_OPERATOR_GROUP + ">" + BOOLEAN_OPERATORS_PATTERN + ") (?<" + BOOL_RIGHT_GROUP + ">.+)");
	private static final Pattern[] MIXED_QUERY_PATTERNS = new Pattern[] {
		Pattern.compile(PHRASE_PATTERN + " (?!" + BOOLEAN_OPERATORS_PATTERN + ")(?<" + KEYWORDS_GROUP + ">.+)(?= #|$)" + "( " + PRF_PATTERN + ")?"),
		Pattern.compile("(?<" + KEYWORDS_GROUP + ">^(?!.+(" + BOOLEAN_OPERATORS_PATTERN + ").+).+) " + PHRASE_PATTERN + "( " + PRF_PATTERN + ")?")
	};
	
	/**
	 * Contains a text preprocessor instance.
	 */
	private final TextPreprocessor textPreprocessor;
	
	
	/**
	 * Creates a new QueryParser instance.
	 * @param textPreprocessor
	 */
	public QueryParser(TextPreprocessor textPreprocessor) {
		this.textPreprocessor = textPreprocessor;
	}


	/**
	 * Parses a given string as query.
	 * @param query
	 * @return
	 * @throws IOException
	 */
	public Query parse(String query) throws IOException {
		// Check, if query is a boolean query
		Matcher matcher = BOOLEAN_QUERY_PATTERN.matcher(query);
		if(matcher.matches()) {
			return this.parseBooleanQuery(matcher);
		}
		
		// Check, if query is a phrase query
		matcher = PHRASE_QUERY_PATTERN.matcher(query);
		if(matcher.matches()) {
			return this.parsePhraseQuery(matcher);
		}
		
		// Check, if query is a mixed query (phrase + keywords)
		for(Pattern pattern: MIXED_QUERY_PATTERNS) {
			matcher = pattern.matcher(query);
			if(matcher.matches()) {
				return this.parseMixedQuery(matcher);
			}
		}
		
		// Default case: keyword query
		return this.parseKeywordQuery(query);
	}
	
	
	/**
	 * Parses a given string as boolean query.
	 * @param matcher
	 * @return
	 * @throws IOException
	 */
	private BooleanQuery parseBooleanQuery(Matcher matcher) throws IOException {
		// Split query into operator and sub queries
		Query leftQuery = this.parse(matcher.group(BOOL_LEFT_GROUP));
		Query rightQuery = this.parse(matcher.group(BOOL_RIGHT_GROUP));
		BooleanOperator operator = BooleanOperator.parse(matcher.group(BOOL_OPERATOR_GROUP));
		
		return new BooleanQuery(leftQuery, rightQuery, operator);
	}
	
	
	/**
	 * Parses a given string as phrase query.
	 * If present, prf parameter is extracted from query.
	 * @param matcher
	 * @return
	 * @throws IOException
	 */
	private PhraseQuery parsePhraseQuery(Matcher matcher) throws IOException {
		int prf = this.extractPrf(matcher);
		
		return this.parsePhraseQuery(matcher, prf);
	}
	
	/**
	 * Parses a given string as phrase query.
	 * Prf parameter can be set manually.
	 * @param matcher
	 * @param prf
	 * @return
	 * @throws IOException
	 */
	private PhraseQuery parsePhraseQuery(Matcher matcher, int prf) throws IOException {
		List<String> phrase = this.tokenize(matcher.group(PHRASE_GROUP));
	
		return new PhraseQuery(phrase, prf);
	}
	
	
	/**
	 * Parses a given string as keyword query.
	 * If present, prf parameter is extracted from query.
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private KeywordQuery parseKeywordQuery(String query) throws IOException {
		// Extract prf parameter
		int prf = 0;
		Matcher matcher = PRF_PATTERN.matcher(query);
		if(matcher.find()) {
			prf = this.extractPrf(matcher);
			
			// Remove prf parameter from query
			query = query.substring(0, matcher.start()).trim();
		}
		
		return this.parseKeywordQuery(query, prf);
	}
	
	/**
	 * Parses a given string as keyword query.
	 * Prf parameter can be set manually.
	 * @param query
	 * @param prf
	 * @return
	 * @throws IOException
	 */
	private KeywordQuery parseKeywordQuery(String query, int prf) throws IOException {
		List<String> keywords = this.tokenize(query);
		
		return new KeywordQuery(keywords, prf);
	}
	
	
	/**
	 * Parses a given string as keyword query.
	 * If present, prf parameter is extracted from query.
	 * @param matcher
	 * @return
	 * @throws IOException
	 */
	private MixedQuery parseMixedQuery(Matcher matcher) throws IOException {
		// Extract prf parameter
		int prf = this.extractPrf(matcher);
		
		// Extract subqueries
		PhraseQuery phraseQuery = this.parsePhraseQuery(matcher, prf);
		KeywordQuery keywordQuery = this.parseKeywordQuery(matcher.group(KEYWORDS_GROUP), prf);
		
		return new MixedQuery(prf, phraseQuery, keywordQuery);
	}
	
	
	/**
	 * Extracts the prf parameter from a query, if present. Returns 0, otherwise.
	 * @param matcher
	 * @return
	 */
	private int extractPrf(Matcher matcher) {
		String prfGroup = matcher.group(PRF_GROUP);
		if(prfGroup != null) {
			return Integer.parseInt(prfGroup);
		}
		
		return 0;
	}
	
	/**
	 * Tokenizes given text and removes stop words.
	 * @param text
	 * @return
	 * @throws IOException
	 */
	private List<String> tokenize(String text) throws IOException {
		return this.textPreprocessor.removeStopWords(this.textPreprocessor.tokenize(text, true));
	}
}
