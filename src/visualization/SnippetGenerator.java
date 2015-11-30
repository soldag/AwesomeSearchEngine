package visualization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parsing.PatentDocument;
import parsing.lookups.PatentAbstractLookup;
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
	private final PatentAbstractLookup abstractLookup;
	
	
	/**
	 * Creates a new SnippetGenerator instance.
	 * @param textPreprocessor
	 * @param abstractLookup
	 */
	public SnippetGenerator(TextPreprocessor textPreprocessor, PatentAbstractLookup abstractLookup) {
		this.textPreprocessor = textPreprocessor;
		this.abstractLookup = abstractLookup;
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
			abstractText = this.abstractLookup.get(document);
			tokenizedAbstract = this.textPreprocessor.tokenize(abstractText);
		} catch (IOException e) {
			return new ArrayList<Snippet>();
		}
		
		// Generate snippet for each query token
		List<Snippet> snippets = new ArrayList<Snippet>();
		Map<String, Integer[]> row = this.getDocumentRow(document, result);
		for(String token: row.keySet()) {
			Integer[] positions = row.get(token);
			if(positions.length > 0) {
				// Get first position of the token
				int position = Arrays.stream(row.get(token))
									.mapToInt(x -> x.intValue())
									.findFirst()
									.getAsInt();
				
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
	
	/**
	 * Extracts the row for the given document from result.
	 * @param document
	 * @param result
	 * @return
	 */
	private Map<String, Integer[]> getDocumentRow(PatentDocument document, QueryResult result) {
		return result.getPostingsTable().rowMap().entrySet().stream()
				.filter(x -> x.getKey().equals(document))
				.map(x -> x.getValue())
				.findAny()
				.orElse(new HashMap<String, Integer[]>());
	}
}
