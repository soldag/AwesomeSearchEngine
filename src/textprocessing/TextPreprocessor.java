package textprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.tartarus.snowball.ext.PorterStemmer;

public class TextPreprocessor {
	
	/**
	 * Contains a set of stop words to be filtered out.
	 */
	private CharArraySet stopWords = CharArraySet.EMPTY_SET;
	
	/**
	 * Contain patent analyzer instances.
	 */
	private PatentAnalyzer defaultAnalyzer;
	private PatentAnalyzer preservingAnalyzer;
	
	/**
	 * Contains a porter stemmer instance.
	 */
	private PorterStemmer stemmer;
	
	
	/**
	 * Creates a new TextPreprocessor instance.
	 */
	public TextPreprocessor() {
		this.defaultAnalyzer = new PatentAnalyzer();
		this.preservingAnalyzer = new PatentAnalyzer(true);
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
		// Get analyzer instance
		PatentAnalyzer analyzer = this.defaultAnalyzer;
		if(preserveWildcards) {
			analyzer = this.preservingAnalyzer;
		}
		
		try(
				TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
			) {
				List<String> tokens = new ArrayList<String>();
				CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		    	tokenStream.reset();			
		    	
				while(tokenStream.incrementToken()) {
		        	tokens.add(termAttribute.toString());
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
		return tokens.stream().filter(x -> !this.isStopWord(x)).collect(Collectors.toList());
	}
	
	/**
	 * Determines, whether the given token is a stop word or not.
	 * @param token
	 * @return
	 */
	public boolean isStopWord(String token) {
		return this.stopWords.contains(token);
	}
}
