package querying;

import java.util.Map;

import com.google.common.collect.ImmutableTable;

import parsing.PatentDocument;
import querying.results.QueryResult;

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
		ImmutableTable.Builder<PatentDocument, String, Integer[]> postingTableBuilder = new ImmutableTable.Builder<PatentDocument, String, Integer[]>();
		result.getPostingsTable().rowMap().entrySet().stream()
				.sorted((doc1, doc2) -> this.compareByWeight(doc1, doc2, result, collectionTokenCount))
				.limit(limit)
				.forEach(row -> row.getValue().entrySet()
						.forEach(column -> postingTableBuilder.put(row.getKey(), column.getKey(), column.getValue())));
		
		return new QueryResult(postingTableBuilder.build(), result.getSpellingCorrections());
	}
	
	/**
	 * Compares two documents by query-likelihood-weight.
	 * @param result
	 * @param collectionTokenCount
	 * @param doc1
	 * @param doc2
	 * @return
	 */
	private int compareByWeight(Map.Entry<PatentDocument, Map<String, Integer[]>> doc1, 
								Map.Entry<PatentDocument, Map<String, Integer[]>> doc2,
								QueryResult result, int collectionTokenCount) {
		
		return Double.compare(
				this.weightDocument(doc1.getKey(), doc1.getValue(), result, collectionTokenCount), 
				this.weightDocument(doc2.getKey(), doc1.getValue(), result, collectionTokenCount));
	}
	
	/**
	 * Calculates the query-likelihood-weight for a given document.
	 * @param document
	 * @param tokenPostings
	 * @param result
	 * @param collectionTokenCount
	 * @return
	 */
	private double weightDocument(PatentDocument document, Map<String, Integer[]> tokenPostings, QueryResult result, int collectionTokenCount) {
		return Math.log(tokenPostings.entrySet().stream()
						.mapToDouble(column -> this.queryLikelihood(
											column.getValue().length, 
											document.getTokensCount(), 
											this.getCollectionFrequency(column.getKey(), result), 
											collectionTokenCount))
						.reduce(1, (x,y) -> x*y));
	}
	
	/**
	 * Counts the numbers of occurrences of the given token in the collection.
	 * @param token
	 * @param result
	 * @return
	 */
	private int getCollectionFrequency(String token, QueryResult result) {
		return result.getPostingsTable().column(token).values().stream().mapToInt(x -> x.length).sum();
	}
	
	/**
	 * Calculates the query-likelihood-ranking for a specific token.
	 * @param tokenDocumentFrequency
	 * @param documentsLength
	 * @param tokenCollectionFrequency
	 * @param collectionLength
	 * @return
	 */
	private double queryLikelihood(int tokenDocumentFrequency, int documentsLength, int tokenCollectionFrequency, int collectionLength) {
		return (1 - QL_LAMBDA) * ((double)tokenDocumentFrequency / (double)documentsLength)
				+ QL_LAMBDA * ((double)tokenCollectionFrequency / (double)collectionLength);
	}
}
