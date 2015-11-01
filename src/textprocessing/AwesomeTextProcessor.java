package textprocessing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.javatuples.Pair;

public class AwesomeTextProcessor {
	
	private static final String STOP_WORD_LIST_FILE_NAME = "StopWords.txt";
	
	// Contains a set of stop words to be filtered out.
	private CharArraySet stopWords = CharArraySet.EMPTY_SET;
	
	
	// Load stop words from file.
	public void loadStopWords() throws IOException {
		List<String> stopWords = new ArrayList<String>();
		try (
			InputStream stream = this.getClass().getResourceAsStream(STOP_WORD_LIST_FILE_NAME);
			BufferedReader br = new BufferedReader(new InputStreamReader(stream))
		) {
    	    String line;
    	    while ((line = br.readLine()) != null) {
    	    	stopWords.add(line);
    	    }
    	}
    	
    	this.stopWords = new CharArraySet(stopWords, true);
	}
	
	public List<Pair<String, Integer>> getTokens(String text) throws IOException {
    	List<Pair<String, Integer>> result = new ArrayList<Pair<String, Integer>>();
    	
		// Build processing queue for tokenizing, stemming and removal of stop words
		StringReader reader = new StringReader(text);
    	try(
			StandardAnalyzer analyzer = new StandardAnalyzer(this.stopWords);
			TokenStream tokenStream = new PorterStemFilter(analyzer.tokenStream(null, reader));
		) {
	    	CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
	    	OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
	    	tokenStream.reset();
	    	
	    	// Create a list of tokens and their offsets
	        while(tokenStream.incrementToken()) {
	        	String token = termAttribute.toString();
	        	int offset = offsetAttribute.startOffset();
	        	result.add(new Pair<String, Integer>(token, offset));
	        }
    	}
        
        return result;
	}
}
