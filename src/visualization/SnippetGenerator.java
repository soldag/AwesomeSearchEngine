package visualization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import documents.PatentContentDocument;
import documents.PatentDocument;
import parsing.PatentContentLookup;
import postings.ContentType;
import postings.DocumentPostings;
import postings.PositionMap;
import querying.results.PrfQueryResult;
import querying.results.QueryResult;
import textprocessing.TextPreprocessor;

public class SnippetGenerator {
	
	/**
	 * Contains a pattern matching a sentence of a text.
	 */
	private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?\\s|$)[^.!?]*)*[.!?]?['\"]?(?=\\s|$)");
	
	/**
	 * Contains a separator, which is used between two snippet sentences, if there is at least another one in between.
	 */
	private static final String SNIPPET_SENTENCE_SEPARATOR = "..";

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
	 * Generates a snippet for the given document of a specific result.
	 * @param document
	 * @param result
	 * @return
	 */
	public Snippet generate(PatentDocument document, QueryResult result) {
		// Get contents of given document
		PatentContentDocument contentDocument;
		try {
			contentDocument = this.patentContentLookup.loadContent(document);
		} catch (IOException e) {
			return null;
		}
		
		return this.generate(contentDocument, result);
	}
	
	/**
	 * Generates a snippet for the given document of a specific result.
	 * @param document
	 * @param result
	 * @return
	 */
	public Snippet generate(PatentContentDocument document, QueryResult result) {
		// If prf is enabled, use original result for snippet generation.
		if(result instanceof PrfQueryResult) {
			result = ((PrfQueryResult)result).getOriginalResult();
		}
		
		// Get abstract text split into each tokens and sentences
		// Map positions of query tokens to abstract sentences
		List<String> tokenizedAbstract;
		String[] abstractSentences;
		Table<Integer, String, List<Integer>> mappedQueryTokens;
		try {
			String abstractText = document.getContent(ContentType.Abstract);
			tokenizedAbstract = this.textPreprocessor.tokenize(abstractText);
			abstractSentences = this.splitIntoSentences(abstractText);

			mappedQueryTokens = this.mapQueryTokens(abstractSentences, result.getPostings().ofDocument(document));
		} catch (IOException e) {
			return new Snippet();
		}

		// Get selection of abstract sentences used for the snippet
		List<Integer> suitableSentencesIndexes = this.getSnippetSentences(mappedQueryTokens);
		if(suitableSentencesIndexes.isEmpty()) {
			// Fallback, if no suitable sentence could be found: first sentence of the abstract
			return new Snippet(abstractSentences[0]);
		}
		else {
			return this.buildSnippet(tokenizedAbstract, suitableSentencesIndexes, abstractSentences, mappedQueryTokens);
		}
	}
	
	/**
	 * Splits given text into list of sentences.
	 * @param text
	 * @return
	 */
	private String[] splitIntoSentences(String text) {
		List<String> sentencesOffsets = new ArrayList<String>();
		Matcher matcher = SENTENCE_PATTERN.matcher(text);
		while(matcher.find()) {
			sentencesOffsets.add(matcher.group());
		}
		
		return sentencesOffsets.stream().toArray(String[]::new);
	}
	
	/**
	 * Gets the start token offsets of each sentence of the given array in the complete text.
	 * @param sentences
	 * @return
	 * @throws IOException
	 */
	private int[] getStartTokenOffsets(String[] sentences) throws IOException {
		int[] sentencePositions = new int[sentences.length];
		int startPosition = 0;
		for(int i = 0; i < sentences.length; i++) {
			sentencePositions[i] = startPosition;
			startPosition += this.textPreprocessor.tokenize(sentences[i]).size();
		}
		
		return sentencePositions;
	}
	
	/**
	 * Maps positions of query tokens to the given sentences.
	 * @param sentences
	 * @param postings
	 * @return Table with sentence indexes as rows, query tokens as columns and corresponding positions as cells.
	 * @throws IOException
	 */
	private Table<Integer, String, List<Integer>> mapQueryTokens(String[] sentences, DocumentPostings postings) throws IOException {		
		// Get sentence start offsets
		int[] startOffsets = this.getStartTokenOffsets(sentences);
		
		// Build mapping table
		Table<Integer, String, List<Integer>> table = HashBasedTable.<Integer, String, List<Integer>>create();
		for(Map.Entry<String, PositionMap> entry: postings.entrySet()) {
			String token = entry.getKey();
			PositionMap positions = entry.getValue();
			
			// If current query token is contained in the abstract of the document, map positions to corresponding sentences
			if(positions.containsContentType(ContentType.Abstract)) {
				for(int position: positions.ofContentType(ContentType.Abstract)) {
					// Find the corresponding sentence id for the position
					for(int i = 0; i < startOffsets.length; i++) {
						if(i == startOffsets.length - 1 || position < startOffsets[i + 1]) {
							if(!table.contains(i, token)) {
								table.put(i, token, new ArrayList<Integer>());								
							}
							table.get(i, token).add(position);
							
							break;
						}
					}
				}
			}
		}
		
		return table;
	}
	
	/**
	 * Selects suitable sentences for the snippet, which cover most of the query tokens.
	 * @param mappedQueryTokens
	 * @return
	 */
	private List<Integer> getSnippetSentences(Table<Integer, String, List<Integer>> mappedQueryTokens) {
		// Clone mapping table
		mappedQueryTokens = HashBasedTable.<Integer, String, List<Integer>>create(mappedQueryTokens);

		List<Integer> sentencesIndexes = new ArrayList<Integer>();
		Set<String> remainingQueryTokens = mappedQueryTokens.columnKeySet();
		while(!remainingQueryTokens.isEmpty()) {
			int maxNewTokens = 0;
			int selectionIndex = -1;
			int selectionTokenOccurrences = 0;
			for(Map.Entry<Integer, Map<String, List<Integer>>> row: mappedQueryTokens.rowMap().entrySet()) {
				int sentenceIndex = row.getKey();
				int newTokenCount = Sets.intersection(remainingQueryTokens, row.getValue().keySet()).size();
				int totalTokenOccurrences = (int)row.getValue().values().stream().flatMap(x -> x.stream()).count();
				
				// Select the sentence with the maximum number of new query tokens (and maximum number of query token occurrences)
				if(newTokenCount > maxNewTokens || (newTokenCount == maxNewTokens && selectionTokenOccurrences < totalTokenOccurrences)) {
					maxNewTokens = newTokenCount;
					selectionIndex = sentenceIndex;
					selectionTokenOccurrences = totalTokenOccurrences;
				}
			}
			
			// Add chosen sentence index to result list
			sentencesIndexes.add(selectionIndex);
			
			// Update list of not covered query tokens
			Set<String> coveredTokens = mappedQueryTokens.row(selectionIndex).keySet();
			remainingQueryTokens = Sets.difference(remainingQueryTokens, coveredTokens).immutableCopy();
			
			// Remove chosen sentence from mapping table
			mappedQueryTokens.row(selectionIndex).clear();
		}
		
		return sentencesIndexes;
	}
	
	/**
	 * Builds the snippet from the selection of suitable sentences.
	 * @param tokenizedAbstract
	 * @param suitableSentencesIndexes
	 * @param abstractSentences
	 * @param mappedQueryTokens
	 * @return
	 */
	private Snippet buildSnippet(List<String> tokenizedAbstract, List<Integer> suitableSentencesIndexes, String[] abstractSentences, Table<Integer, String, List<Integer>> mappedQueryTokens) {
		int lastSentenceIndex = -1;
		StringBuilder snippetBuilder = new StringBuilder();
		Set<String> snippetQueryTokens = new HashSet<String>();
		for(int sentenceIndex: suitableSentencesIndexes) {
			// If previous sentence is not part of the sentence, insert separator
			if(lastSentenceIndex > 0 && lastSentenceIndex != sentenceIndex - 1) {
				snippetBuilder.append(SNIPPET_SENTENCE_SEPARATOR);
			}
			
			// Append sentence to snippet text
			snippetBuilder.append(abstractSentences[sentenceIndex]);
			
			// Add all query tokens within the current sentence
			snippetQueryTokens.addAll(mappedQueryTokens.row(sentenceIndex).values().stream()
															.flatMap(positions -> positions.stream())
															.map(position -> tokenizedAbstract.get(position))
															.collect(Collectors.toSet()));
			
			lastSentenceIndex = sentenceIndex;
		}
		
		return new Snippet(snippetBuilder.toString(), snippetQueryTokens);
	}
}
