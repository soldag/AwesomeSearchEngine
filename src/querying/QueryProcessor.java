package querying;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.Sets;

import documents.PatentDocument;
import indexing.documentmap.DocumentMapReader;
import indexing.documentmap.DocumentMapSeekList;
import indexing.invertedindex.InvertedIndexReader;
import indexing.invertedindex.InvertedIndexSeekList;
import io.FileFactory;
import io.FileReader;
import postings.ContentType;
import postings.DocumentPostings;
import postings.PositionMap;
import postings.PostingTable;
import querying.results.QueryResult;
import querying.results.PrfQueryResult;
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
	private boolean isCompressed;
	
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
	 * @param isCompressed
	 * @throws FileNotFoundException
	 */
	public QueryProcessor(TextPreprocessor textProcessor, DamerauLevenshteinCalculator damerauLevenshtein, DocumentRanker documentRanker, SnippetGenerator snippetGenerator,
			File documentMapFile, File documentMapSeekListFile, File indexFile, File indexSeekListFile, boolean isCompressed) throws FileNotFoundException {
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
		this.isCompressed = isCompressed;
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
		this.documentMapReader = new DocumentMapReader(documentMapFile, this.isCompressed);
		try(FileReader seekListReader = FileFactory.getInstance().getReader(this.documentMapSeekListFile, this.isCompressed)) {
			this.documentMapSeekList.load(seekListReader);
		}
		
		this.indexReader = new InvertedIndexReader(this.indexFile, this.isCompressed);
		try(FileReader seekListReader = FileFactory.getInstance().getReader(this.indexSeekListFile, this.isCompressed)) {
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
		QueryResult result1 = this.searchToken(tokens.get(0));
		QueryResult result2 = this.searchToken(tokens.get(1));
		
		return QueryResult.conjunct(result1, result2);
	}
	
	/**
	 * Search for disjunction of given tokens in the document collection.
	 * @param tokens
	 * @return
	 * @throws IOException
	 */
	private QueryResult searchOr(List<String> tokens) throws IOException {
		QueryResult[] results = tokens.stream()
									.map(token -> this.searchToken(token))
									.toArray(QueryResult[]::new);
		
		return QueryResult.disjunct(results);
	}
	
	/**
	 * Search for negated conjunction of given tokens in the document collection.
	 * @param tokens
	 * @return
	 * @throws IOException
	 */
	private QueryResult searchNot(List<String> tokens) throws IOException {
		// Search for single tokens separately
		QueryResult result1 = this.searchToken(tokens.get(0));
		QueryResult result2 = this.searchToken(tokens.get(1));
		
		return QueryResult.conjunctNegated(result1, result2);
	}
	
	/**
	 * Searched for the phrase of given tokens in the document collection. Only document, containing tokens in the given order are returned.
	 * @param tokens
	 * @param topK
	 * @return
	 * @throws IOException
	 */
	private QueryResult searchPhrase(List<String> tokens, int topK) throws IOException {
		PostingTable resultTokenPostings = null;
		PostingTable lastTokenPostings = null;
		Map<String, String> spellingCorrections = null;
		
		for(String token: tokens) {
			// Search for token
			QueryResult result = this.searchToken(token);
			
			if(resultTokenPostings == null) {
				// Add all postings of first token
				resultTokenPostings = lastTokenPostings = result.getPostings();
				spellingCorrections = result.getSpellingCorrections();
			}
			else {
				// Filter out those tokens, that are not part of the phrase and store valid postings in a new table
				Set<Integer> documentIds = result.getPostings().documentIdSet();
				PostingTable newTokenPostings = new PostingTable();
				for(int documentId: documentIds) {
					DocumentPostings tokenPositions = result.getPostings().ofDocument(documentId);
					for(String currentToken: tokenPositions.tokenSet()) {
						PositionMap positionMap = tokenPositions.ofToken(currentToken);
						for(ContentType contentType: positionMap.contentTypeSet()) {
							int[] currentPositions = positionMap.ofContentType(contentType);
							if(lastTokenPostings.containsDocument(documentId)) {
								int[] lastPositions = lastTokenPostings.ofDocument(documentId).positions().stream()
														.flatMapToInt(x -> Arrays.stream(x.ofContentType(contentType)))
														.toArray();
								
								if(this.areSuccessive(lastPositions, currentPositions)) {
									newTokenPostings.put(currentToken, documentId, contentType, currentPositions);
								}
							}
						}
					}
				}
	
				// Remove invalid, existing entries from result
				Set<Integer> abadonedDocumentIds = Sets.difference(resultTokenPostings.documentIdSet(), newTokenPostings.documentIdSet()).immutableCopy();
				for(int documentId: abadonedDocumentIds) {
					resultTokenPostings.remove(documentId);
				}
				
				// Insert new, valid postings to result
				resultTokenPostings.putAll(newTokenPostings);
				if(resultTokenPostings.isEmpty()) {
					// If no phrases were found, return empty result
					return new QueryResult();
				}
				
				// Add spelling corrections
				spellingCorrections.putAll(result.getSpellingCorrections());
				
				// Overwrite lastPostings with new ones for the next iteration
				lastTokenPostings = newTokenPostings; 
			}
		}
		
		// If no phrases were found, return empty result
		if(resultTokenPostings == null) {
			return new QueryResult();
		}

		// Weight documents
		resultTokenPostings.loadDocuments(this::getDocument);
		QueryResult result = new QueryResult(resultTokenPostings, spellingCorrections);
		result = this.documentRanker.weightResult(result, this.indexReader.getTotalTokenCount(), topK);
		
		return result;
	}
	
	/**
	 * Determines, whether position1 contains at least one position that is precedessor of a position if positions2. 
	 * @param posting1
	 * @param posting2
	 * @return boolean
	 */
	private boolean areSuccessive(int[] positions1, int[] positions2) {
		return Arrays.stream(positions1)
					.anyMatch(pos1 -> Arrays.stream(positions2)
								.anyMatch(pos2 -> pos2 == pos1 + 1));
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
		QueryResult result = this.keywordSearch(tokens, topK);
		
		// If enabled, extend query using pseudo relevance feedback
		if(prf > 0) {
			return this.extendPrf(tokens, topK, prf, result);
		}
		
		return result;
	}
	
	/**
	 * Extend the query by most frequent tokens of the snippets of the top documents of the original query (pseudo relevance feedback).
	 * @param tokens
	 * @param topK
	 * @param prf
	 * @param originalResult
	 * @return
	 * @throws IOException
	 */
	private PrfQueryResult extendPrf(List<String> tokens, int topK, int prf, QueryResult originalResult) throws IOException {
		originalResult.getPostings().loadDocuments(this::getDocument);
		List<String> additionalTerms = originalResult.getPostings().documentSet().stream()
										.limit(prf)
										.map(document -> this.snippetGenerator.generate(document, originalResult))
										.flatMap(x -> this.getMostFrequentTokens(
														x.stream()
																.flatMap(y -> this.textPreprocessor.removeStopWords(y.getTokens()).stream())
																.collect(Collectors.toList()),
														PRF_K).stream())
										.collect(Collectors.toList());
		tokens.addAll(additionalTerms);
		tokens = tokens.stream().distinct().collect(Collectors.toList());
		return PrfQueryResult.fromResults(this.keywordSearch(tokens, topK), originalResult);
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
				.sorted(MapValueComparator.natural())
				.map(x -> x.getKey())
				.distinct()
				.limit(limit)
				.collect(Collectors.toList());
	}
	
	/**
	 * Runs a keyword search for a list of query tokens and weights the result.
	 * @param tokens
	 * @param topK
	 * @return List of patent documents.
	 * @throws IOException
	 */
	private QueryResult keywordSearch(List<String> tokens, int topK) throws IOException {
		// Search for tokens
		QueryResult result = this.searchOr(tokens);
		
		// Weight documents
		result.getPostings().loadDocuments(this::getDocument);
		result = this.documentRanker.weightResult(result, this.indexReader.getTotalTokenCount(), topK);
		
		return result;
	}
	
	/**
	 * Gets all postings of a given token from index.
	 * @param token
	 * @return 
	 */
	private QueryResult searchToken(String token) {
		return this.searchToken(token, null);
	}
	
	/**
	 * Gets all postings of a given (corrected) token from index. 
	 * The original misspelled token has to be passed as second argument.
	 * @param token
	 * @param misspelledToken
	 * @return 
	 */
	private QueryResult searchToken(String token, String misspelledToken) {
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
			PostingTable postings = this.indexReader.getPostings(token, startOffset, prefixSearch);
			
			// Spelling correction
			if(!prefixSearch && postings.isEmpty()) {
				String correctedToken = this.spellingCorrector.correctToken(token); 
				if(correctedToken != null) {
					return searchToken(correctedToken, token);
				}
			}
			
			// If token was corrected, include correction map to result
			if(misspelledToken != null) {				
				Map<String, String> spellingCorrections = new HashMap<String, String>();
				spellingCorrections.put(token, misspelledToken);
				
				return new QueryResult(postings, spellingCorrections);				
			}
			
			return new QueryResult(postings);
			
		}
		catch(IOException e) {
			return new QueryResult();
		}
	}
	
	/**
	 * Retrieves the PatentDocument from document map by specifying its id.
	 * @param documentId
	 * @return
	 */
	private PatentDocument getDocument(int documentId) {
		if(documentId == 6836939) {
			System.out.println("");
		}
		int startOffset = this.documentMapSeekList.getIndexOffset(documentId);
		try {
			return this.documentMapReader.getDocument(documentId, startOffset);
		} catch (IOException e) {
			return null;
		}
	}
}
