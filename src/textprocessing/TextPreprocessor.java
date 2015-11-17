package textprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.tartarus.snowball.ext.PorterStemmer;

public class TextPreprocessor {
	
	/**
	 * Contains the wildcard character used for prefix searches.
	 */
	private static final String WILDCARD_CHARACTER = "*";
	
	/**
	 * Contains a set of stop words to be filtered out.
	 */
	private CharArraySet stopWords = CharArraySet.EMPTY_SET;
	
	/**
	 * Contains a analyzer for patent texts.
	 */
	private PatentAnalyzer analyzer;
	
	/**
	 * Contains a porter stemmer instance.
	 */
	private PorterStemmer stemmer;
	
	
	/**
	 * Creates a new TextPreprocessor instance.
	 */
	public TextPreprocessor() {
		this.analyzer = new PatentAnalyzer();
		this.stemmer = new PorterStemmer();
	}
	
	
	/**
	 * Load stop words from file.
	 * @throws IOException
	 */
	public void loadStopWords(File stopWordList) throws IOException {
		List<String> stopWords = new ArrayList<String>();
		try (
			BufferedReader br = new BufferedReader(new FileReader(stopWordList))
		) {
    	    String line;
    	    while ((line = br.readLine()) != null) {
    	    	stopWords.add(line);
    	    }
    	}
    	
    	this.stopWords = new CharArraySet(stopWords, true);
	}
	
	/**
	 * Tokenizes the given text. Wildcard characters are removed.
	 * @param text
	 * @return
	 * @throws IOException
	 */
	public List<String> tokenize(String text) throws IOException {
		return this.tokenize(text, false);
	}
	
	/**
	 * Tokenizes the given text. Depending on preserveWildcards, wildcard characters are preserved or removed.
	 * @param text
	 * @param preserveWildcards
	 * @return
	 * @throws IOException
	 */
	public List<String> tokenize(String text, boolean preserveWildcards) throws IOException {
		// Check for wildcard characters
		HashSet<String> wildcardTerms = new HashSet<String>();
		if(preserveWildcards) {
			Pattern pattern = Pattern.compile(String.format("\\w+(?=%s)", Pattern.quote(WILDCARD_CHARACTER)));
			Matcher matcher = pattern.matcher(text);
			while(matcher.find()) {
				wildcardTerms.add(matcher.group());
			}
		}
		
		try(TokenStream tokenStream = this.analyzer.tokenStream(null, new StringReader(text))) {
			List<String> tokens = new ArrayList<String>();
			CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
	    	tokenStream.reset();			
	    	
			while(tokenStream.incrementToken()) {
	        	String token = termAttribute.toString();
	        	if(preserveWildcards && wildcardTerms.contains(token)) {
	        		token += WILDCARD_CHARACTER;
	        	}
	        	tokens.add(token);
			}
			tokenStream.end();
			
			return tokens;
		}
	}
	
	/**
	 * Stems a given token.
	 * @param token
	 * @return Stemmed token
	 */
	public String stem(String token) {
		this.stemmer.setCurrent(token);
		if(this.stemmer.stem()) {
			return this.stemmer.getCurrent();
		}
		
		return token;
	}
	
	/**
	 * Removes stop words from a list of tokens.
	 * @param tokens
	 * @return List of tokens without stop words.
	 */
	public List<String> removeStopWords(List<String> tokens) {
		return tokens.stream().filter(x -> !this.stopWords.contains(x)).collect(Collectors.toList());
	}
}
