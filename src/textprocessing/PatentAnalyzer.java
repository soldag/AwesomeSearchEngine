package textprocessing;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class PatentAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		StandardTokenizer tokenizer = new StandardTokenizer();
		TokenStream tokenStream = new StandardFilter(tokenizer);
		tokenStream = new LowerCaseFilter(tokenStream);
		tokenStream = new EnglishPossessiveFilter(tokenStream);
		
		return new TokenStreamComponents(tokenizer, tokenStream);
	}

}
