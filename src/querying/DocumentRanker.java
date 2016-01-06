package querying;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableTable;

import documents.PatentDocument;
import postings.ContentType;
import postings.DocumentPostings;
import postings.PositionMap;
import postings.PostingTable;
import querying.results.PrfQueryResult;
import querying.results.QueryResult;
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
	 * Weights a query result using query-likelihood-model. Resulting query result is limited to 'documentLimit' entries.
	 * @param result
	 * @param resultLimit
	 * @param collectionTokenCount
	 * @return
	 */
	public QueryResult weightResult(QueryResult result, int resultLimit, int collectionTokenCount) {
		PostingTable resultPostings = result.getPostings();
		
		// If result should not be limited, set resultLimit to number of resulting documents
		if(resultLimit < 0) {
			resultLimit = resultPostings.documentIdSet().size();
		}
		
		// Calculate weights for each document
		Map<PatentDocument, Double> weights = resultPostings.documentSet().stream()
													.collect(Collectors.toMap(
															Function.identity(), 
															document -> this.weightDocument(document, result, collectionTokenCount)));
		
		// Sort documents by weight and limit to given parameter
		List<PatentDocument> rankedDocuments = weights.entrySet().stream()
													.sorted(MapValueComparator.reverse())
													.limit(resultLimit)
													.map(e -> e.getKey())
													.collect(Collectors.toList());
		
		// Creates ranked posting table
		ImmutableTable.Builder<String, Integer, PositionMap> postingTableBuilder = new ImmutableTable.Builder<String, Integer, PositionMap>();
		rankedDocuments
				.forEach(document -> resultPostings.ofDocument(document).tokenSet().stream()
						.forEach(token -> postingTableBuilder.put(token, document.getId(), resultPostings.ofDocument(document).ofToken(token))));
		
		// Create new document map limited to ranked documents
		Map<Integer, PatentDocument> documents = rankedDocuments.stream()
														.collect(Collectors.toMap(document -> document.getId(), Function.identity()));
		
		return new QueryResult(new PostingTable(postingTableBuilder.build(), documents), result.getSpellingCorrections());
	}
	
	/**
	 * Calculates the query-likelihood-weight for a given document.
	 * @param document
	 * @param tokenPostings
	 * @param result
	 * @param collectionTokenCount
	 * @return
	 */	
	private double weightDocument(PatentDocument document, QueryResult result, int collectionTokenCount) {
		return Arrays.stream(ContentType.values())
				.mapToDouble(contentType -> contentType.getWeightingFactor() * this.weightDocument(document, contentType, result, collectionTokenCount))
				.sum();
	}
	
	/**
	 * Calculates the query-likelihood-weight for a specific content type of a given document.
	 * @param document
	 * @param tokenPostings
	 * @param result
	 * @param collectionTokenCount
	 * @return
	 */	
	private double weightDocument(PatentDocument document, ContentType contentType, QueryResult result, int collectionTokenCount) {
		DocumentPostings documentPostings = result.getPostings().ofDocument(document);
		if(documentPostings.positions().stream().anyMatch(positionMap -> positionMap.containsContentType(contentType))) {
			return result.getPostings().tokenSet().stream()
							.mapToDouble(token -> this.getPrfFactor(token, result) * 
												  this.queryLikelihood(
													this.countTokenOccurrences(token, documentPostings, contentType), 
													document.getTokensCount(contentType), 
													this.getCollectionFrequency(token, result), 
													collectionTokenCount))
							.reduce(1, (x,y) -> x*y);
		}
		
		return 0;
	}
	
	/**
	 * Counts the number of token occurrences in the specified content of the given postings 
	 * @param token
	 * @param documentPostings
	 * @param contentType
	 * @return
	 */
	private int countTokenOccurrences(String token, DocumentPostings documentPostings, ContentType contentType) {
		if(documentPostings.containsToken(token) && documentPostings.ofToken(token).containsContentType(contentType)) {
			return documentPostings.ofToken(token).ofContentType(contentType).length;
		}
		
		return 0;
	}
	
	/**
	 * Returns a factor for weighting of tokens.
	 * @param token
	 * @param result
	 * @return
	 */
	private double getPrfFactor(String token, QueryResult result) {
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
	 * Counts the numbers of occurrences of the given token in the collection.
	 * @param token
	 * @param result
	 * @return
	 */
	private int getCollectionFrequency(String token, QueryResult result) {
		return result.getPostings().ofToken(token).positions().stream().mapToInt(x -> x.size()).sum();
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
