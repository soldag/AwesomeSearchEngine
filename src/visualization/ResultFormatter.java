package visualization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import documents.PatentContentDocument;
import documents.PatentDocument;
import parsing.PatentContentLookup;
import postings.ContentType;
import postings.DocumentPostings;
import querying.results.QueryResult;
import querying.results.RankedQueryResult;
import textprocessing.TextPreprocessor;

public class ResultFormatter {
	
	/**
	 * Contains necessary services.
	 */
	private PatentContentLookup patentContentLookup;
	private final TextPreprocessor textPreprocessor;
	private SnippetGenerator snippetGenerator;
	
	
	/**
	 * Creates a new ResultFormatter instance.
	 * @param patentContentLookup
	 * @param textPreprocessor
	 * @param snippetGenerator
	 */
	public ResultFormatter(PatentContentLookup patentContentLookup, TextPreprocessor textPreprocessor, SnippetGenerator snippetGenerator) {
		this.patentContentLookup = patentContentLookup;
		this.textPreprocessor = textPreprocessor;
		this.snippetGenerator = snippetGenerator;
	}
	

	/**
	 * Formats the given result for outputting on console. Resulting lists contains formatted string for each document in the result.
	 * @param result
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> format(RankedQueryResult result) throws IOException {
		ArrayList<String> formattedResults = new ArrayList<String>();
		for(PatentDocument document: result.getRankedDocuments()) {
			// Load contents of document
			PatentContentDocument contentDocument = this.patentContentLookup.loadContent(document);
			
			// Get properties
			int id = document.getId();
			String title = contentDocument.getContent(ContentType.Title);
			Snippet snippet = this.snippetGenerator.generate(contentDocument, result);
			
			// Format properties
			StringBuilder resultBuilder = new StringBuilder();
			resultBuilder.append(id);
			resultBuilder.append(" ");
			resultBuilder.append(this.formatTitle(title, document, result));
			resultBuilder.append(System.getProperty("line.separator"));
			resultBuilder.append(snippet.toFormattedString());
			
			formattedResults.add(resultBuilder.toString());
		}
		
		return formattedResults;
	}
	
	/**
	 * Highlights query tokens and formats the whole title bold.
	 * @param title
	 * @param document
	 * @param result
	 * @return
	 */
	private String formatTitle(String title, PatentDocument document, QueryResult result) {
		// Tokenize title
		List<String> tokenizedTitle;
		try {
			tokenizedTitle = this.textPreprocessor.tokenize(title);
		} catch (IOException e) {
			return title;
		}
		
		// Get positions of query tokens in title
		DocumentPostings postings = result.getPostings().ofDocument(document.getId());
		if(postings != null) {
			int[] queryTokenPositions = postings.positions().stream()
													.filter(positionMap -> positionMap.containsContentType(ContentType.Title))
													.flatMapToInt(positionMap -> Arrays.stream(positionMap.ofContentType(ContentType.Title)))
													.toArray();
			
			// Highlight query tokens in title
			for(int position: queryTokenPositions) {
				String token = tokenizedTitle.get(position);
				title = title.replaceAll("\\b" + token + "\\b", ResultStyle.ANSI_COLOR_GREEN + token + ResultStyle.ANSI_COLOR_RESET);
			}
		}
		
		// Format whole title bold
		title = ResultStyle.ANSI_BOLD + title + ResultStyle.ANSI_BOLD_RESET;
		
		return title;
	}
}
