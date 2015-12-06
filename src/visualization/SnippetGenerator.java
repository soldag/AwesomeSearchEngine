package visualization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import documents.PatentDocument;
import parsing.PatentContentLookup;
import postings.ContentType;
import postings.DocumentPostings;
import querying.results.PrfQueryResult;
import querying.results.QueryResult;
import textprocessing.TextPreprocessor;

public class SnippetGenerator {
	
	/**
	 * Determines, how many words before and after a query token will be extracted for the snippet.
	 */
	private static final int MAX_QUERY_TOKEN_DISTANCE = 3;

	/**
	 * Contain necessary services.
	 */
	private final TextPreprocessor textPreprocessor;
	private final PatentContentLookup patentContentLookup;
	
	
	/**
	 * Creates a new SnippetGenerator instance.
	 * @param textPreprocessor
	 * @param patentContentLookup
	 */
	public SnippetGenerator(TextPreprocessor textPreprocessor, PatentContentLookup patentContentLookup) {
		this.textPreprocessor = textPreprocessor;
		this.patentContentLookup = patentContentLookup;
	}
	
	
	/**
	 * Generates a list of snippets for the given document of a specific result.
	 * @param document
	 * @param result
	 * @return
	 */
	public List<Snippet> generate(PatentDocument document, QueryResult result) {
		// If prf is enabled, use original result for snippet generation.
		if(result instanceof PrfQueryResult) {
			result = ((PrfQueryResult)result).getOriginalResult();
		}
		
		// Get abstract text
		String abstractText;
		List<String> tokenizedAbstract;
		try {
			abstractText = this.patentContentLookup.get(document, ContentType.Abstract);
			tokenizedAbstract = this.textPreprocessor.tokenize(abstractText);
		} catch (IOException e) {
			return new ArrayList<Snippet>();
		}
		
		// Generate snippet for each query token
		List<Snippet> snippets = new ArrayList<Snippet>();
		DocumentPostings tokenPositions = result.getPostings().ofDocument(document);
		for(String token: tokenPositions.tokenSet()) {
			int[] positions = tokenPositions.ofToken(token).ofContentType(ContentType.Abstract);
			if(positions.length > 0) {
				// Get first position of the token
				int position = positions[0];
				
				// Create snippet
				Snippet snippet = new Snippet();
				for(int i = position - MAX_QUERY_TOKEN_DISTANCE; i <= position + MAX_QUERY_TOKEN_DISTANCE; i++) {
					if(i >= 0 && i < tokenizedAbstract.size()) {
						snippet.add(tokenizedAbstract.get(i), i == position);
					}
				}
				snippets.add(snippet);
			}
		}
		
		return snippets;
	}
}
