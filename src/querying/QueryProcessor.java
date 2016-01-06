package querying;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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
import querying.queries.BooleanQuery;
import querying.queries.KeywordQuery;
import querying.queries.MixedQuery;
import querying.queries.PhraseQuery;
import querying.queries.PrfQuery;
import querying.queries.Query;
import querying.queries.QueryParser;
import querying.results.PrfQueryResult;
import querying.spellingcorrection.DamerauLevenshteinCalculator;
import querying.spellingcorrection.SpellingCorrector;
import textprocessing.TextPreprocessor;
import utilities.MapValueComparator;
import visualization.SnippetGenerator;

public class QueryProcessor {
	
	/**
	 * Contains the number of most frequent tokens that are used for pseudo-relevance-feedback.
	 */
	private static final int PRF_MOST_FREQUENT_TOKENS = 10;
	
	/**
	 * Contains necessary services.
	 */
	private QueryParser queryParser;
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
	 * @param queryParser
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
	public QueryProcessor(QueryParser queryParser, TextPreprocessor textProcessor, DamerauLevenshteinCalculator damerauLevenshtein, DocumentRanker documentRanker, 
			SnippetGenerator snippetGenerator, File documentMapFile, File documentMapSeekListFile, File indexFile, File indexSeekListFile, boolean isCompressed) throws FileNotFoundException {
		this.queryParser = queryParser;
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
	 * Gets a boolean, that determines, whether the index is compressed, or not.
	 * @return
	 */
	public boolean isCompressed() {
		return this.isCompressed;
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
	 * Searches for a given query string in the document collection.
	 * Result is limited to 'resultLimit' documents.
	 * @param queryString
	 * @param resultLimit
	 * @return
	 * @throws IOException
	 */
	public QueryResult search(String queryString, int resultLimit) throws IOException {
		Query query = this.queryParser.parse(queryString);
		return this.search(query, resultLimit, true);
	}
	
	/**
	 * Searches for a given query in the document collection.
	 * @param query
	 * @param resultLimit Number of documents, that should be present at most in the result. If this value is less than 0, all relevant documents are returned. 
	 * @param weightDocuments Determines, whether the result should be weighted or not. 
	 * @return
	 * @throws IOException
	 */
	private QueryResult search(Query query, int resultLimit, boolean weightDocuments) throws IOException {
		// Search for query depending on its type
		QueryResult result;
		switch(query.getType()) {
			case BooleanQuery.TYPE:
				result = this.search((BooleanQuery)query);
				break;
				
			case PhraseQuery.TYPE:
				result = this.search((PhraseQuery)query);
				break;
				
			case MixedQuery.TYPE:
				result = this.search((MixedQuery)query);
				break;
				
			case KeywordQuery.TYPE:
				result = this.search((KeywordQuery)query);
				break;
				
			default: 
				result = new QueryResult();
				break;
		}
		
		// If enabled, extend query using pseudo relevance feedback (only supported for keyword queries)
		if(query instanceof PrfQuery) {
			PrfQuery prfQuery = (PrfQuery)query;
			if(prfQuery.getPrf() > 0) {
				PrfQuery extendedQuery = this.extendPrfQuery(prfQuery, result);
				result = PrfQueryResult.fromResults(this.search(extendedQuery, resultLimit, false), result);
			}
		}
		
		// Load documents of result
		result.getPostings().loadDocuments(this::getDocument);
		
		// Weight resulting documents, if specified so
		if(weightDocuments && query.getType() != BooleanQuery.TYPE) {
			result = this.documentRanker.weightResult(result, resultLimit, this.indexReader.getTotalTokenCount());
		}
		
		return result;
	}
	
	/**
	 * Searches for multiple queries and return result array (unweighted).
	 * @param queries
	 * @return
	 * @throws IOException
	 */
	private QueryResult[] searchAllUnweighted(Query... queries) throws IOException {
		QueryResult[] results = new QueryResult[queries.length];
		for(int i = 0; i < queries.length; i++) {
			results[i] = this.search(queries[i], -1, false);
		}
		
		return results;
	}
	
	
	/**
	 * Evaluates the given boolean query. 
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private QueryResult search(BooleanQuery query) throws IOException {
		switch(query.getOperator()) {		
			case Or:
				return QueryResult.disjunct(this.searchAllUnweighted(query.getLeftQuery(), query.getRightQuery()));
				
			case And:
				return QueryResult.conjunct(this.searchAllUnweighted(query.getLeftQuery(), query.getRightQuery()));
				
			case Not:
				return QueryResult.relativeComplement(this.searchAllUnweighted(query.getLeftQuery(), query.getRightQuery()));
				
			default:
				return new QueryResult();
		}
	}
	
	
	/**
	 * Searched for a phrase of tokens in the document collection. Only document, containing the tokens in the given order are returned.
	 * @param tokens
	 * @return
	 * @throws IOException
	 */
	private QueryResult search(PhraseQuery query) throws IOException {
		PostingTable resultTokenPostings = null;
		PostingTable lastTokenPostings = null;
		Map<String, String> spellingCorrections = null;
		
		for(String token: query.getQueryTokens()) {
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
		
		return new QueryResult(resultTokenPostings, spellingCorrections);
	}
	
	/**
	 * Determines, whether position1 contains at least one position that is predecessor of a position if positions2. 
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
	 * Evaluates the given mixed query.
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private QueryResult search(MixedQuery query) throws IOException {
		Query[] queries = query.getQueries();
		return QueryResult.disjunct(this.searchAllUnweighted(queries));
	}
	
	
	/**
	 * Evaluates the given keyword query. 
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private QueryResult search(KeywordQuery query) throws IOException {
		// Search for tokens
		List<String> queryTokens = query.getQueryTokens();
		QueryResult[] results = new QueryResult[queryTokens.size()];
		for(int i = 0; i < queryTokens.size(); i++) {
			results[i] = this.searchToken(queryTokens.get(i));
		}
		
		return QueryResult.disjunct(results);
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
	 * Gets all postings of a given (possibly corrected) token from index. 
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
	 * Extend the query by most frequent tokens of the snippets of the top documents of the original query (pseudo relevance feedback).
	 * @param query
	 * @param originalResult
	 * @return
	 * @throws IOException
	 */
	private PrfQuery extendPrfQuery(final PrfQuery query, QueryResult originalResult) throws IOException {
		originalResult.getPostings().loadDocuments(this::getDocument);
		List<String> additionalTokens = originalResult.getPostings().documentSet().stream()
										.limit(query.getPrf())
										.map(document -> this.snippetGenerator.generate(document, originalResult))
										.flatMap(x -> this.getMostFrequentTokens(x.toString(), PRF_MOST_FREQUENT_TOKENS).stream())
										.filter(token -> !query.containsToken(token))
										.collect(Collectors.toList());
		
		
		return query.extendBy(additionalTokens);
	}
	
	/**
	 * Returns the most frequent tokens of a text limited to 'limit'.
	 * @param text
	 * @param limit
	 * @return
	 */
	private List<String> getMostFrequentTokens(String text, int limit) {
		List<String> tokens;
		try {
			tokens = this.textPreprocessor.removeStopWords(this.textPreprocessor.tokenize(text));
		} catch (IOException e) {
			return new ArrayList<String>();
		}
		
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
