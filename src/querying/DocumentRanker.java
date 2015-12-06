package querying;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableTable;

import documents.PatentDocument;
import postings.ContentType;
import postings.DocumentPostings;
import postings.PositionMap;
import postings.PostingTable;
import querying.results.QueryResult;
import utilities.MapValueComparator;

public class DocumentRanker {
	
	/**
	 * Contains the lambda factor for jelinek-mercer smoothing.
	 */
	private static final double QL_LAMBDA = 0.2;
	
	/**
	 * Weights a query result using query-likelihood-model. Resulting query result is limited to 'limit' entries.
	 * @param result
	 * @param collectionTokenCount
	 * @param limit
	 * @return
	 */
	public QueryResult weightResult(QueryResult result, int collectionTokenCount, int limit) {
		PostingTable resultPostings = result.getPostings();
		
		// Calculate weights for each document
		Map<PatentDocument, Double> weights = resultPostings.documentSet().stream()
													.collect(Collectors.toMap(
															Function.identity(), 
															document -> this.weightDocument(document, result, collectionTokenCount)));
		
		// Sort documents by weight and limit to given parameter
		List<PatentDocument> rankedDocuments = weights.entrySet().stream()
													.sorted(MapValueComparator.reverse())
													.limit(limit)
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
		return Math.log(Arrays.stream(ContentType.values())
				.mapToDouble(contentType -> contentType.getWeightingFactor() * this.weightDocument(document, contentType, result, collectionTokenCount))
				.sum());
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
		DocumentPostings tokenPostings = result.getPostings().ofDocument(document);
		if(tokenPostings.positions().stream().anyMatch(positionMap -> positionMap.containsContentType(contentType))) {
			return result.getPostings().tokenSet().stream()
							.filter(token -> tokenPostings.containsToken(token) && tokenPostings.ofToken(token).containsContentType(contentType))
							.mapToDouble(token -> this.queryLikelihood(
													tokenPostings.ofToken(token).ofContentType(contentType).length, 
													document.getTokensCount(contentType), 
													this.getCollectionFrequency(token, result), 
													collectionTokenCount))
							.reduce(1, (x,y) -> x*y);
		}
		
		return 0;
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
