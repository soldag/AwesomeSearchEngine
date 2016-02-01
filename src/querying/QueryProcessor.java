package querying;

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

import indexing.citations.CitationIndexReader;
import indexing.invertedindex.InvertedIndexReader;
import postings.ContentType;
import postings.DocumentPostings;
import postings.PostingTable;
import postings.positions.EagerPositionMap;
import postings.positions.PositionMap;
import querying.results.UnrankedQueryResult;
import querying.queries.BooleanQuery;
import querying.queries.KeywordQuery;
import querying.queries.LinkToQuery;
import querying.queries.MixedQuery;
import querying.queries.PhraseQuery;
import querying.queries.PrfQuery;
import querying.queries.Query;
import querying.queries.QueryParser;
import querying.ranking.DocumentRanker;
import querying.results.QueryResult;
import querying.results.RankedQueryResult;
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
	private SpellingCorrector spellingCorrector;
	private SnippetGenerator snippetGenerator;
	
	/**
	 * Contains necessary index reader services.
	 */
	private InvertedIndexReader invertedIndexReader;
	private CitationIndexReader citationIndexReader;
	
	/**
	 * Creates a new QueryProcessor instance.
	 * @param invertedIndexReader
	 * @param documentMapReader
	 * @param queryParser
	 * @param textProcessor
	 * @param spellingCorrector
	 * @param documentRanker
	 * @param snippetGenerator
	 * @param citationIndexReader
	 * @throws FileNotFoundException
	 */
	public QueryProcessor(InvertedIndexReader invertedIndexReader, CitationIndexReader citationIndexReader, QueryParser queryParser, TextPreprocessor textProcessor, 
			SpellingCorrector spellingCorrector, DocumentRanker documentRanker, SnippetGenerator snippetGenerator) throws FileNotFoundException {
		this.queryParser = queryParser;
		this.textPreprocessor = textProcessor;
		this.documentRanker = documentRanker;
		this.spellingCorrector = spellingCorrector;
		this.snippetGenerator = snippetGenerator;
		this.invertedIndexReader = invertedIndexReader;
		this.citationIndexReader = citationIndexReader;
	}
	
	
	/**
	 * Searches for a given query string in the document collection.
	 * Result is limited to 'resultLimit' documents.
	 * @param queryString
	 * @param resultLimit
	 * @return
	 * @throws IOException
	 */
	public RankedQueryResult search(String queryString, int resultLimit) throws IOException {
		Query query = this.queryParser.parse(queryString);
		return this.search(query, resultLimit);
	}
	
	/**
	 * Searches for a given query in the document collection and weights resulting documents.
	 * @param query
	 * @param resultLimit Number of documents, that should be present at most in the result. If this value is less than 0, all relevant documents are returned. 
	 * @return
	 * @throws IOException
	 */
	private RankedQueryResult search(Query query, int resultLimit) throws IOException {
		QueryResult unrankedResult = this.searchUnweighted(query, resultLimit);
		
		// Rank result depending on query type
		RankedQueryResult result;
		if(query.getType() == BooleanQuery.TYPE || query.getType() == LinkToQuery.TYPE) {
			result = this.documentRanker.limitResult(unrankedResult, resultLimit);
		}
		else {
			result = this.documentRanker.weightResult(unrankedResult, resultLimit, this.invertedIndexReader.getTotalTokenCount());
		}
		
		// If enabled, extend query using pseudo relevance feedback (only supported for keyword queries)
		if(query instanceof PrfQuery) {
			PrfQuery prfQuery = (PrfQuery)query;
			if(prfQuery.getPrf() > 0) {
				PrfQuery extendedQuery = this.extendPrfQuery(prfQuery, result);
				unrankedResult = UnrankedQueryResult.fromResults(this.searchUnweighted(extendedQuery, resultLimit), result);
				result = this.documentRanker.weightResult(unrankedResult, resultLimit, this.invertedIndexReader.getTotalTokenCount());
			}
		}
		
		return result;
	}
	
	/**
	 * Searches for a given query in the document collection without weighting resulting documents.
	 * @param query
	 * @param resultLimit
	 * @return
	 * @throws IOException
	 */
	private UnrankedQueryResult searchUnweighted(Query query, int resultLimit) throws IOException {
		// Search for query depending on its type
		UnrankedQueryResult result;
		switch(query.getType()) {
			case BooleanQuery.TYPE:
				result = this.search((BooleanQuery)query, resultLimit);
				break;
				
			case PhraseQuery.TYPE:
				result = this.search((PhraseQuery)query);
				break;
				
			case LinkToQuery.TYPE:
				result = this.search((LinkToQuery)query);
				break;
				
			case MixedQuery.TYPE:
				result = this.search((MixedQuery)query);
				break;
				
			case KeywordQuery.TYPE:
				result = this.search((KeywordQuery)query);
				break;
				
			default: 
				result = new UnrankedQueryResult();
				break;
		}
		
		return result;
	}
	
	/**
	 * Searches for multiple queries and return result array (unweighted).
	 * @param queries
	 * @return
	 * @throws IOException
	 */
	private UnrankedQueryResult[] searchAllUnweighted(Query... queries) throws IOException {
		UnrankedQueryResult[] results = new UnrankedQueryResult[queries.length];
		for(int i = 0; i < queries.length; i++) {
			results[i] = this.searchUnweighted(queries[i], -1);
		}
		
		return results;
	}
	
	
	/**
	 * Evaluates the given boolean query. 
	 * @param query
	 * @param resultLimit
	 * @return
	 * @throws IOException
	 */
	private UnrankedQueryResult search(BooleanQuery query, int resultLimit) throws IOException {
		UnrankedQueryResult result;
		switch(query.getOperator()) {		
			case Or:
				result = UnrankedQueryResult.disjunct(this.searchAllUnweighted(query.getLeftQuery(), query.getRightQuery()));
				break;
				
			case And:
				result = UnrankedQueryResult.conjunct(this.searchAllUnweighted(query.getLeftQuery(), query.getRightQuery()));
				break;
				
			case Not:
				result = UnrankedQueryResult.relativeComplement(this.searchAllUnweighted(query.getLeftQuery(), query.getRightQuery()));
				break;
				
			default:
				return new UnrankedQueryResult();
		}
		
		return result;
	}
	
	
	/**
	 * Searched for a phrase of tokens in the document collection. Only document, containing the tokens in the given order are returned.
	 * @param tokens
	 * @return
	 * @throws IOException
	 */
	private UnrankedQueryResult search(PhraseQuery query) throws IOException {
		PostingTable resultTokenPostings = null;
		PostingTable lastTokenPostings = null;
		Map<String, String> spellingCorrections = null;
		
		for(String token: query.getQueryTokens()) {
			// Search for token
			UnrankedQueryResult result = this.searchToken(token);
			
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
									PositionMap currentPositionMap = new EagerPositionMap();
									positionMap.put(contentType, currentPositions);
									
									newTokenPostings.put(currentToken, documentId, currentPositionMap);
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
					return new UnrankedQueryResult();
				}
				
				// Add spelling corrections
				spellingCorrections.putAll(result.getSpellingCorrections());
				
				// Overwrite lastPostings with new ones for the next iteration
				lastTokenPostings = newTokenPostings; 
			}
		}
		
		// If no phrases were found, return empty result
		if(resultTokenPostings == null) {
			return new UnrankedQueryResult();
		}
		
		return new UnrankedQueryResult(resultTokenPostings, spellingCorrections);
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
	 * Evaluates the given link to query.
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private UnrankedQueryResult search(LinkToQuery query) throws IOException {
		int queryDocumentId = query.getDocumentId();
		Set<Integer> linkingDocumentIds = this.citationIndexReader.getLinkingDocuments(queryDocumentId);
		
		return new UnrankedQueryResult(linkingDocumentIds);
	}
	
	
	/**
	 * Evaluates the given mixed query.
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private UnrankedQueryResult search(MixedQuery query) throws IOException {
		Query[] queries = query.getQueries();
		return UnrankedQueryResult.disjunct(this.searchAllUnweighted(queries));
	}
	
	
	/**
	 * Evaluates the given keyword query. 
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private UnrankedQueryResult search(KeywordQuery query) throws IOException {
		// Search for tokens
		List<String> queryTokens = query.getQueryTokens();
		UnrankedQueryResult[] results = new UnrankedQueryResult[queryTokens.size()];
		for(int i = 0; i < queryTokens.size(); i++) {
			results[i] = this.searchToken(queryTokens.get(i));
		}
		
		return UnrankedQueryResult.disjunct(results);
	}
	
	
	/**
	 * Gets all postings of a given token from index.
	 * @param token
	 * @return 
	 */
	private UnrankedQueryResult searchToken(String token) {
		return this.searchToken(token, null);
	}
	
	/**
	 * Gets all postings of a given (possibly corrected) token from index. 
	 * The original misspelled token has to be passed as second argument.
	 * @param token
	 * @param misspelledToken
	 * @return 
	 */
	private UnrankedQueryResult searchToken(String token, String misspelledToken) {
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
			PostingTable postings = this.invertedIndexReader.getPostings(token, prefixSearch, false);
			
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
				
				return new UnrankedQueryResult(postings, spellingCorrections);				
			}
			
			return new UnrankedQueryResult(postings);
			
		}
		catch(IOException e) {
			return new UnrankedQueryResult();
		}
	}
	
	
	/**
	 * Extend the query by most frequent tokens of the snippets of the top documents of the original query (pseudo relevance feedback).
	 * @param query
	 * @param originalResult
	 * @return
	 * @throws IOException
	 */
	private PrfQuery extendPrfQuery(final PrfQuery query, RankedQueryResult originalResult) throws IOException {
		List<String> additionalTokens = originalResult.getRankedDocuments().stream()
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
}
