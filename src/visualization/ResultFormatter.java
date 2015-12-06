package visualization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import documents.PatentDocument;
import parsing.PatentContentLookup;
import postings.ContentType;
import querying.results.QueryResult;

public class ResultFormatter {
	
	/**
	 * Contains ANSI constants for formatting console output.
	 */
	private static final String ANSI_BOLD = "\033[1m";
	private static final String ANSI_BOLD_RESET = "\033[0m";
	
	/**
	 * Contains necessary services.
	 */
	private PatentContentLookup patentContentLookup;
	private SnippetGenerator snippetGenerator;
	
	
	/**
	 * Creates a new ResultFormatter instance.
	 * @param titleLookup
	 * @param snippetGenerator
	 */
	public ResultFormatter(PatentContentLookup patentContentLookup, SnippetGenerator snippetGenerator) {
		this.patentContentLookup = patentContentLookup;
		this.snippetGenerator = snippetGenerator;
	}
	

	/**
	 * Formats the given result for outputting on console. Resulting lists contains formatted string for each document in the result.
	 * @param result
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> format(QueryResult result) throws IOException {
		ArrayList<String> formattedResults = new ArrayList<String>();
		for(PatentDocument document: result.getPostings().documentSet()) {
			// Get properties
			int id = document.getId();
			String title = this.patentContentLookup.get(document, ContentType.Title);
			String snippet = this.snippetGenerator.generate(document, result).stream()
								.map(x -> x.toFormattedString())
								.collect(Collectors.joining("..."));
			
			// Format properties
			StringBuilder resultBuilder = new StringBuilder();
			resultBuilder.append(id);
			resultBuilder.append(" ");
			resultBuilder.append(ANSI_BOLD + title + ANSI_BOLD_RESET);
			resultBuilder.append(System.getProperty("line.separator"));
			resultBuilder.append(snippet);
			resultBuilder.append(System.getProperty("line.separator"));
			
			formattedResults.add(resultBuilder.toString());
		}
		
		return formattedResults;
	}
}
