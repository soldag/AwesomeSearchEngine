package querying;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;

import indexing.Posting;
import indexing.documentmap.DocumentMapReader;
import indexing.documentmap.DocumentMapSeekList;
import indexing.invertedindex.InvertedIndexReader;
import indexing.invertedindex.InvertedIndexSeekList;
import parsing.PatentDocument;
import querying.results.IntermediateQueryResult;
import querying.results.PrfQueryResult;
import querying.results.QueryResult;
import querying.spellingcorrection.DamerauLevenshteinCalculator;
import querying.spellingcorrection.SpellingCorrector;
import textprocessing.TextPreprocessor;
import utilities.MapValueComparator;
import visualization.SnippetGenerator;

public class QueryProcessor {
	
	/**
	 * Contains a list of supported boolean operators.
	 */
	private static final List<String> BOOLEAN_OPERATORS = Arrays.asList("AND", "OR", "NOT");
	
	/**
	 * Contains the number of most used tokens that are used for pseudo-relevance-feedback.
	 */
	private static final int PRF_K = 10;
	
	/**
	 * Contains necessary services.
	 */
	private TextPreprocessor textPreprocessor;
	private DocumentRanker documentRanker;
	private DamerauLevenshteinCalculator damerauLevenshtein;
	private SpellingCorrector spellingCorrector;
	private SnippetGenerator snippetGenerator;
	
	/**
	 * Contain necessary index files and reader services.
	 */
	private File documentMapFile;
	private DocumentMapReader documentMapReader;
	private File documentMapSeekListFile;
	private DocumentMapSeekList documentMapSeekList;

	private File indexFile;
	private InvertedIndexReader indexReader;
	private File indexSeekListFile;
	private InvertedIndexSeekList indexSeekList;
	
	/**
	 * Determines, whether the index is compressed, or not.
	 */
	private boolean compressed;
	
	/**
	 * Determines, whether the index has been loaded and the instance is ready for querying.
	 */
	private boolean isReady = false;
	
	/**
	 * Creates a new QueryProcessor instance.
	 * @param textProcessor
	 * @param damerauLevenshtein
	 * @param documentRanker
	 * @param documentMapFile
	 * @param documentMapSeekListFile
	 * @param indexFile
	 * @param indexSeekListFile
	 * @param compressed
	 * @throws FileNotFoundException
	 */
	public QueryProcessor(TextPreprocessor textProcessor, DamerauLevenshteinCalculator damerauLevenshtein, DocumentRanker documentRanker, SnippetGenerator snippetGenerator,
			File documentMapFile, File documentMapSeekListFile, File indexFile, File indexSeekListFile, 
			boolean compressed) throws FileNotFoundException {
		this.textPreprocessor = textProcessor;
		this.damerauLevenshtein = damerauLevenshtein;
		this.documentRanker = documentRanker;
		this.snippetGenerator = snippetGenerator;
		
		this.documentMapFile = documentMapFile;
		this.documentMapSeekListFile = documentMapSeekListFile;
		this.documentMapSeekList = new DocumentMapSeekList();
		
		this.indexFile = indexFile;
		this.indexSeekListFile = indexSeekListFile;
		this.indexSeekList = new InvertedIndexSeekList();		
		this.compressed = compressed;
	}

	
	/**
	 * Gets a boolean, that determines, whether the index has been loaded and the instance is ready for querying.
	 * @return
	 */
	public boolean isReady() {
		return this.isReady;
	}
	
	/**
	 * Loads the seek lists from disk to memory in order to perform queries.
	 * @throws IOException
	 */
	public void load() throws IOException {
		this.documentMapReader = new DocumentMapReader(documentMapFile);
		try(RandomAccessFile seekListReader = new RandomAccessFile(this.documentMapSeekListFile, "r")) {
			this.documentMapSeekList.load(seekListReader);
		}
		
		this.indexReader = new InvertedIndexReader(this.indexFile, this.compressed);
		try(RandomAccessFile seekListReader = new RandomAccessFile(this.indexSeekListFile, "r")) {
			this.indexSeekList.load(seekListReader);
		}

		this.spellingCorrector = new SpellingCorrector(this.damerauLevenshtein, this.indexReader, this.indexSeekList);
		
		this.isReady = true;
	}
	
	/**
	 * Search for a given query in the document collection.
	 * @param query
	 * @param topK
	 * @return
	 * @throws IOException
	 */
	public QueryResult search(String query, int topK, int prf) throws IOException {
		String mode = "";
		List<String> tokens = this.textPreprocessor.tokenize(query, true);
		String booleanOperator = this.getBooleanOperator(tokens);
		if(booleanOperator != null) {
			tokens.remove(booleanOperator);
			mode = booleanOperator.toUpperCase();
		}
		else if(query.startsWith("'") && query.endsWith("'")) {
			mode = "PHRASE";
		}
		
		tokens = this.textPreprocessor.removeStopWords(tokens);
		
		switch(mode) {
			case "AND":
				return this.searchAnd(tokens);
				
			case "OR":
				return this.searchOr(tokens);
				
			case "NOT":
				return this.searchNot(tokens);
				
			case "PHRASE":
				return this.searchPhrase(tokens, topK);
				
			default:
				return this.searchKeywords(tokens, topK, prf);
		}
	}
	
	/**
	 * Extracts the boolean operator from a tokenized query. If no operator is present, null is returned.
	 * @param tokens
	 * @return
	 */
	private String getBooleanOperator(List<String> tokens) {
		if(tokens.size() == 3) {
			String operator = tokens.get(1);
			if(BOOLEAN_OPERATORS.contains(operator.toUpperCase())) {
				return operator;
			}
		}
		
		return null;
	}
	
	/**
	 * Search for conjunction of given tokens in the document collection.
	 * @param tokens
	 * @return
	 * @throws IOException
	 */
	private QueryResult searchAnd(List<String> tokens) throws IOException {
		// Search for single tokens separately
		IntermediateQueryResult result1 = this.searchToken(tokens.get(0));
		IntermediateQueryResult result2 = this.searchToken(tokens.get(1));
		
		// Calculate intersection of documents
		Map<PatentDocument, Map<String, Integer[]>> rowMap = result1.getPostingsTable().rowMap().entrySet().stream()
			.filter(row -> result2.getPostingsTable().rowMap().containsKey(row.getKey()))
			.collect(Collectors.toMap(row -> this.getDocument(row.getKey()), row -> row.getValue()));
		
		// Merge spelling corrections
		Map<String, String> spellingCorrections = result1.getSpellingCorrections();
		spellingCorrections.putAll(result2.getSpellingCorrections());
		
		return new QueryResult(rowMap, spellingCorrections);
	}
	
	/**
	 * Search for disjunction of given tokens in the document collection.
	 * @param tokens
	 * @return
	 * @throws IOException
	 */
	private QueryResult searchOr(List<String> tokens) throws IOException {		
		return this.getResult(tokens.stream()
								.map(x -> this.searchToken(x))
								.reduce((x,y) -> IntermediateQueryResult.merge(x, y))
								.get());
	}
	
	/**
	 * Search for negated conjunction of given tokens in the document collection.
	 * @param tokens
	 * @return
	 * @throws IOException
	 */
	private QueryResult searchNot(List<String> tokens) throws IOException {
		// Search for single tokens separately
		IntermediateQueryResult result1 = this.searchToken(tokens.get(0));
		IntermediateQueryResult result2 = this.searchToken(tokens.get(1));

		// Calculate negated conjunction of documents
		Map<PatentDocument, Map<String, Integer[]>> rowMap = result1.getPostingsTable().rowMap().entrySet().stream()
			.filter(row -> !result2.getPostingsTable().rowMap().containsKey(row.getKey()))
			.collect(Collectors.toMap(row -> this.getDocument(row.getKey()), row -> row.getValue()));

		// Merge spelling corrections
		Map<String, String> spellingCorrections = result1.getSpellingCorrections();
		spellingCorrections.putAll(result2.getSpellingCorrections());
		
		return new QueryResult(rowMap, spellingCorrections);
	}
	
	/**
	 * Searched for the phrase of given tokens in the document collection. Only document, containing tokens in the given order are returned.
	 * @param tokens
	 * @param topK
	 * @return
	 * @throws IOException
	 */
	private QueryResult searchPhrase(List<String> tokens, int topK) throws IOException {
		Map<Integer, Integer[]> lastPostings = new HashMap<Integer, Integer[]>();
		HashBasedTable<Integer, String, Integer[]> postingTable = HashBasedTable.<Integer, String, Integer[]>create();
		Map<String, String> spellingCorrections = new HashMap<String, String>();
		
		for(String token: tokens) {
			// Search for token
			IntermediateQueryResult result = this.searchToken(token);
			
			// Filter out those tokens, that are not part of the phrase and store valid postings in a new table
			final Map<Integer, Integer[]> lastPostingsFinal = lastPostings;
			HashBasedTable<Integer, String, Integer[]> newPostings = HashBasedTable.<Integer, String, Integer[]>create();
			result.getPostingsTable().cellSet().stream()
					.filter(cell -> lastPostingsFinal.isEmpty() || lastPostingsFinal.containsKey(cell.getRowKey()) && this.areSuccessive(lastPostingsFinal.get(cell.getRowKey()), cell.getValue()))
					.forEach(cell -> newPostings.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
			
			// Remove invalid, existing entries from result
			Map<Integer, Map<String, Integer[]>> rowMap = new HashMap<Integer, Map<String, Integer[]>>(postingTable.rowMap());
			rowMap.entrySet().stream()
					.filter(row -> !newPostings.rowKeySet().contains(row.getKey()))
					.forEach(row -> row.getValue().keySet().stream()
							.forEach(column -> postingTable.remove(row.getKey(), column)));
			
			// Insert new, valid postings to result
			postingTable.putAll(newPostings);
			
			// Add spelling corrections
			spellingCorrections.putAll(result.getSpellingCorrections());
			
			// Overwrite lastPostings with new ones for the next iteration
			lastPostings = newPostings.columnMap().values().stream()
					.flatMap(x -> x.entrySet().stream())
					.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		}
		
		// Assure, that spelling corrections only include tokens that are part of the posting table
		Set<String> postingsTokens = postingTable.columnKeySet();
		Set<String> correctedTokens = spellingCorrections.keySet();
		correctedTokens.stream()
				.filter(x -> !postingsTokens.contains(x))
				.forEach(x -> spellingCorrections.remove(x));
		
		// Create final intermediate result
		IntermediateQueryResult intermediateResult = new IntermediateQueryResult(postingTable, spellingCorrections);
		
		// Weight documents
		QueryResult result = this.getResult(intermediateResult);
		result = this.documentRanker.weightResult(result, this.indexReader.getTotalTokenCount(), topK);
		
		return result;
	}
	
	/**
	 * Determines, whether position1 contains at least one position that is precedessor of a position if positions2. 
	 * @param posting1
	 * @param posting2
	 * @return boolean
	 */
	private boolean areSuccessive(Integer[] positions1, Integer[] positions2) {
		return Arrays.stream(positions1)
					.anyMatch(pos1 -> Arrays.stream(positions2)
								.anyMatch(pos2 -> pos2.equals(pos1 + 1)));
	}
	
	/**
	 * Searches for a given keyword query. Results of query tokens are combined(disjunction) and weighted.
	 * @param tokens
	 * @param topK
	 * @param prf
	 * @return
	 * @throws IOException 
	 */
	private QueryResult searchKeywords(List<String> tokens, int topK, int prf) throws IOException {
		QueryResult result = this.simpleSearch(tokens, topK);
		
		// Extend query by most frequent tokens of top documents (pseudo relevance), if enabled
		if(prf > 0) {
			List<String> additionalTerms = result.getPostingsTable().rowKeySet().stream()
											.limit(prf)
											.map(x -> this.snippetGenerator.generate(x, result))
											.flatMap(x -> this.getMostFrequentTokens(
															x.stream()
																	.flatMap(y -> this.textPreprocessor.removeStopWords(y.getTokens()).stream())
																	.collect(Collectors.toList()),
															PRF_K).stream())
											.collect(Collectors.toList());
			tokens.addAll(additionalTerms);
			tokens = tokens.stream().distinct().collect(Collectors.toList());
			return PrfQueryResult.fromResults(this.simpleSearch(tokens, topK), result);
		}
		
		return result;
	}
	
	/**
	 * Returns the most frequent tokens from a list limited to 'limit'.
	 * @param tokens
	 * @param limit
	 * @return
	 */
	private List<String> getMostFrequentTokens(List<String> tokens, int limit) {
		Map<String, Integer> tokenCounts = new HashMap<String, Integer>();
		for(String token: tokens) {
			tokenCounts.putIfAbsent(token, 0);
			tokenCounts.put(token, tokenCounts.get(token) + 1);
		}
		
		return tokenCounts.entrySet().stream()
				.sorted(new MapValueComparator<String, Integer>())
				.map(x -> x.getKey())
				.distinct()
				.limit(limit)
				.collect(Collectors.toList());
	}
	
	/**
	 * Runs a simple search for a list of query tokens and weights the result.
	 * @param tokens
	 * @param topK
	 * @return List of patent documents.
	 * @throws IOException
	 */
	private QueryResult simpleSearch(List<String> tokens, int topK) throws IOException {
		// Search for tokens
		QueryResult result = this.getResult(tokens.stream()
											.map(x -> this.searchToken(x))
											.reduce((x,y) -> IntermediateQueryResult.merge(x, y))
											.get());
		
		// Weight documents
		result = this.documentRanker.weightResult(result, this.indexReader.getTotalTokenCount(), topK);
		
		return result;
	}
	
	/**
	 * Gets all postings of a given token from index.
	 * @param token
	 * @return 
	 */
	private IntermediateQueryResult searchToken(String token) {
		return this.searchToken(token, null);
	}
	
	/**
	 * Gets all postings of a given (corrected) token from index. 
	 * The original misspelled token has to be passed as second argument.
	 * @param token
	 * @param misspelledToken
	 * @return 
	 */
	private IntermediateQueryResult searchToken(String token, String misspelledToken) {
		try {
			// Stem token or remove wildcard character (if prefix search)
			boolean prefixSearch = token.endsWith("*");
			if(prefixSearch) {
				token = token.substring(0, token.length() - 1);
			}
			else {
				token = this.textPreprocessor.stem(token);
			}
			
			// Get postings
			int startOffset = this.indexSeekList.getIndexOffset(token);
			Map<String, List<Posting>> postings = this.indexReader.getPostings(token, startOffset, prefixSearch);
			
			// Spelling correction
			if(!prefixSearch && postings.isEmpty()) {
				String correctedToken = this.spellingCorrector.correctToken(token); 
				if(correctedToken != null) {
					return searchToken(correctedToken, token);
				}
			}
			
			return this.getResult(postings, misspelledToken);
		}
		catch(IOException e) {
			return new IntermediateQueryResult();
		}
	}

	/**
	 * Creates a new IntermediateQueryResult from a map of postings.
	 * @param postings
	 * @return
	 */
	private IntermediateQueryResult getResult(Map<String, List<Posting>> postings, String misspelledToken) {
		// Construct postings table
		HashBasedTable<Integer, String, Integer[]> postingsTable = HashBasedTable.<Integer, String, Integer[]>create();
		postings.entrySet().stream()
		.forEach(tokenPostings -> tokenPostings.getValue().stream()
			.collect(Collectors.groupingBy(posting -> posting.getDocumentId()))
			.entrySet().stream()
			.forEach(documentPostings -> postingsTable.put(
											documentPostings.getKey(), 
											tokenPostings.getKey(), 
											documentPostings.getValue().stream()
												.flatMap(x -> Arrays.stream(x.getPositions()).boxed())
												.toArray(Integer[]::new))));

		
		// Construct spelling correction map, if token was corrected
		if(misspelledToken != null) {
			Map<String, String> spellingCorrections = postings.keySet().stream()
															.collect(Collectors.toMap(x -> x, x -> misspelledToken));
			
			return new IntermediateQueryResult(postingsTable, spellingCorrections);
		}
		
		return new IntermediateQueryResult(postingsTable);
	}
	
	/**
	 * Converts a IntermediateQueryResult to a QueryResult instance.
	 * @param intermediateResult
	 * @return
	 */
	private QueryResult getResult(IntermediateQueryResult intermediateResult) {
		// Replace document ids with corresponding PatentDocument instances.
		Map<PatentDocument, Map<String, Integer[]>> rowMap = 
				intermediateResult.getPostingsTable().rowMap().entrySet().stream()
					.collect(Collectors.toMap(row -> this.getDocument(row.getKey()), row -> row.getValue()));
		
		return new QueryResult(rowMap, intermediateResult.getSpellingCorrections());
	}
	
	/**
	 * Retrieves the PatentDocument from document map by specifying its id.
	 * @param documentId
	 * @return
	 */
	private PatentDocument getDocument(int documentId) {
		int startOffset = this.documentMapSeekList.getIndexOffset(documentId);
		try {
			return this.documentMapReader.getDocument(documentId, startOffset);
		} catch (IOException e) {
			return null;
		}
	}
}
