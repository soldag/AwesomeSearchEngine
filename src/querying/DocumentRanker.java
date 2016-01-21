package querying;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import documents.PatentDocument;
import indexing.documentmap.DocumentMapReader;
import postings.ContentType;
import postings.DocumentPostings;
import postings.PostingTable;
import postings.positions.PositionMap;
import querying.results.PrfQueryResult;
import querying.results.RankedQueryResult;
import querying.results.UnrankedQueryResult;
import utilities.MapValueComparator;

public class DocumentRanker {
	
	/**
	 * Contains the lambda factor for jelinek-mercer smoothing.
	 */
	private static final double QL_LAMBDA = 0.2;
	
	/**
	 * Contains a factor for weights of tokens, that are not part of the original query (when using prf)
	 */
	private static final double NON_QUERY_TOKEN_FACTOR = 0.25;
	
	/**
	 * Contains the document map reader service.
	 */
	private DocumentMapReader documentMapReader;
	
	
	/**
	 * Creates a new DocumentRanker instance.
	 * @param documentMapReader
	 */
	public DocumentRanker(DocumentMapReader documentMapReader) {
		this.documentMapReader = documentMapReader;
	}
	
	
	/**
	 * Weights a query result using query-likelihood-model. Resulting query result is limited to 'documentLimit' entries.
	 * @param result
	 * @param resultLimit
	 * @param collectionTokenCount
	 * @return
	 */
	public RankedQueryResult weightResult(UnrankedQueryResult result, int resultLimit, int collectionTokenCount) {
		PostingTable resultPostings = result.getPostings();
		
		// If result should not be limited, set resultLimit to number of resulting documents
		if(resultLimit < 0) {
			resultLimit = resultPostings.documentIdSet().size();
		}
		
		// Narrow range of document to the ones, which contain most of the query tokens
		Set<Integer> rankingDocumentIds = resultPostings.documentIdSet();
		if(rankingDocumentIds.size() > resultLimit) {
			int queryTokensCount = resultPostings.tokenSet().size();
			for(int i = queryTokensCount; i > 0; i--)
			{
				final int count = i;
				Set<Integer> documents = resultPostings.documentIdSet().stream()
													.filter(document -> resultPostings.ofDocument(document).entrySet().size() >= count)
													.collect(Collectors.toSet());
				if(documents.size() >= resultLimit || i == 1) {
					rankingDocumentIds = documents;
					break;
				}
			}
		}
		
		// Determine collection frequencies of query tokens
		Map<String, Integer> collectionFrequencies = resultPostings.tokenSet().stream()
															.collect(Collectors.toMap(
																	Function.identity(), 
																	token -> this.getCollectionFrequency(token, result)));
		
		// Calculate weights for each document
		Map<PatentDocument, Double> weights = rankingDocumentIds.stream()
													.map(this::loadDocument)
													.collect(Collectors.toMap(
															Function.identity(), 
															document -> this.weightDocument(document, result, collectionFrequencies, collectionTokenCount)));
		
		// Sort documents by weight and limit to given parameter
		List<PatentDocument> rankedDocuments = weights.entrySet().stream()
													.sorted(MapValueComparator.reverse())
													.limit(resultLimit)
													.map(e -> e.getKey())
													.collect(Collectors.toList());
		
		// Creates ranked posting table
		PostingTable postingTable = new PostingTable();
		for(PatentDocument document: rankedDocuments) {
			DocumentPostings documentPostings = resultPostings.ofDocument(document.getId());
			postingTable.putAll(document, documentPostings);
		}
		
		return new RankedQueryResult(postingTable, result.getSpellingCorrections(), rankedDocuments);
	}
	
	/**
	 * Counts the numbers of occurrences of the given token in the collection.
	 * @param token
	 * @param result
	 * @return
	 */
	private int getCollectionFrequency(String token, UnrankedQueryResult result) {
		return result.getPostings().ofToken(token).getTotalOccurencesCount();
	}
	
	/**
	 * Loads the specified document from document map.
	 * @param documentId
	 * @return
	 */
	private PatentDocument loadDocument(int documentId) {
		try {
			return this.documentMapReader.getDocument(documentId);
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Calculates the query-likelihood-weight for a given document.
	 * @param document
	 * @param tokenPostings
	 * @param result
	 * @param collectionFrequencies
	 * @param collectionTokenCount
	 * @return
	 */	
	private double weightDocument(PatentDocument document, UnrankedQueryResult result, Map<String, Integer> collectionFrequencies, int collectionTokenCount) {
		return Arrays.stream(ContentType.values())
				.mapToDouble(contentType -> contentType.getWeightingFactor() * 
											this.weightDocument(document, contentType, result,collectionFrequencies, collectionTokenCount))
				.sum();
	}
	
	/**
	 * Calculates the query-likelihood-weight for a specific content type of a given document.
	 * @param document
	 * @param tokenPostings
	 * @param result
	 * @param collectionFrequencies
	 * @param collectionTokenCount
	 * @return
	 */	
	private double weightDocument(PatentDocument document, ContentType contentType, UnrankedQueryResult result, Map<String, Integer> collectionFrequencies, int collectionTokenCount) {
		DocumentPostings documentPostings = result.getPostings().ofDocument(document.getId());
		return result.getPostings().tokenSet().stream()
						.mapToDouble(token -> this.getPrfFactor(token, result) * 
											  this.queryLikelihood(
												this.countTokenOccurrences(token, documentPostings, contentType), 
												document.getTokensCount(contentType), 
												collectionFrequencies.get(token), 
												collectionTokenCount))
						.reduce(1, (x,y) -> x*y);
	}
	
	/**
	 * Returns a factor for weighting of tokens.
	 * @param token
	 * @param result
	 * @return
	 */
	private double getPrfFactor(String token, UnrankedQueryResult result) {
		if(result instanceof PrfQueryResult) {
			PrfQueryResult prfResult = (PrfQueryResult)result;
			Set<String> originalQueryTokens = prfResult.getOriginalResult().getPostings().tokenSet();
			if(!originalQueryTokens.contains(token)) {
				return NON_QUERY_TOKEN_FACTOR;
			}
		}
		
		return 1;
	}
	
	/**
	 * Counts the number of token occurrences in the specified content of the given postings 
	 * @param token
	 * @param documentPostings
	 * @param contentType
	 * @return
	 */
	private int countTokenOccurrences(String token, DocumentPostings documentPostings, ContentType contentType) {
		PositionMap positionMap = documentPostings.ofToken(token);
		if(positionMap != null) {
			return positionMap.size(contentType);
		}
		
		return 0;
	}
	
	/**
	 * Calculates the query-likelihood-ranking for a specific token.
	 * @param tokenDocumentFrequency
	 * @param documentsLength
	 * @param tokenCollectionFrequency
	 * @param collectionLength
	 * @return
	 */
	private double queryLikelihood(double tokenDocumentFrequency, double documentsLength, double tokenCollectionFrequency, double collectionLength) {
		return (1 - QL_LAMBDA) * (tokenDocumentFrequency / documentsLength) + QL_LAMBDA * (tokenCollectionFrequency / collectionLength);
	}
}
