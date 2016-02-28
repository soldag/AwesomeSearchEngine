package textprocessing;

import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;

public class PatentAnalyzer extends Analyzer {

	/**
	 * Contains the regex for tokens while preserving wildcard characters.
	 */
	private static final String PRESERVE_WILDCARDS_REGEX = "([a-zA-Z-_]+\\*?)";
	
	/**
	 * Contains the regex for tokens while removing wildcard characters.
	 */
	private static final String REMOVE_WILDCARDS_REGEX = "([a-zA-Z-_]+)";
	
	/**
	 * Contains the tokenizer pattern for the current analyzer.
	 */
	private Pattern tokenizerPattern;
	
	
	/**
	 * Creates a new PatentAnalyzer instance, that ignores wildcard characters.
	 */
	public PatentAnalyzer() {
		this(false);
	}
	
	/**
	 * Create a new PatentAnalyzer instance, that preserves wildcard characters.
	 * @param preserveWildcards
	 */
	public PatentAnalyzer(boolean preserveWildcards) {
		super();
		
		if(preserveWildcards) {
			this.tokenizerPattern = Pattern.compile(PRESERVE_WILDCARDS_REGEX);
		}
		else {
			this.tokenizerPattern = Pattern.compile(REMOVE_WILDCARDS_REGEX);
		}
	}

	
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		PatternTokenizer tokenizer = new PatternTokenizer(this.tokenizerPattern, 0);
		TokenStream tokenStream = new StandardFilter(tokenizer);
		tokenStream = new LengthFilter(tokenStream, 2, Integer.MAX_VALUE);
		tokenStream = new LowerCaseFilter(tokenStream);
		tokenStream = new EnglishPossessiveFilter(tokenStream);
		
		return new TokenStreamComponents(tokenizer, tokenStream);
	}

}
