package querying;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import parsing.PatentDocument;
import utilities.MapValueComparator;

public class DocumentRanker {
	
	/**
	 * Contains the lambda factor for jelinek-mercer smoothing.
	 */
	private static final double QL_LAMBDA = 0.2;

	/**
	 * Weights a list of documents for a given query using query-likelihood-model. Resulting list is limited to topK entries.
	 * @param documents
	 * @param documentFrequencies
	 * @param collectionFrequencies
	 * @param topK
	 * @return Weighted list of documents
	 * @throws IOException
	 */
	public List<PatentDocument> weightResult(List<PatentDocument> documents, Map<String, Map<Integer, Integer>> documentFrequencies, Map<String, Integer> collectionFrequencies, int totalTokenCount, int topK) throws IOException {
		HashMap<PatentDocument, Double> weightedDocuments = new HashMap<PatentDocument, Double>();
		for(PatentDocument document: documents) {
			double weight = Math.log(documentFrequencies.keySet().stream()
									.mapToDouble(token -> this.queryLikelihood(
															this.getDocumentFrequency(token, document.getId(), documentFrequencies),
															document.getTokensCount(), 
															collectionFrequencies.get(token), 
															totalTokenCount))
									.reduce(1, (x,y) -> x*y));
			weightedDocuments.put(document, weight);
		}
		
		// Sort result by weight descending and limit results by topK
		return weightedDocuments.entrySet().stream()
				.sorted(Collections.reverseOrder(new MapValueComparator<PatentDocument, Double>()))
				.limit(topK)
				.map(x -> x.getKey())
				.collect(Collectors.toList());
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
	
	/**
	 * Extracts the document frequency of a given token in a specific document from the documentFrequencies map.
	 * @param token
	 * @param documentId
	 * @param documentFrequencies
	 * @return Number of occurrences of the given token in the specific document.
	 */
	private int getDocumentFrequency(String token, int documentId, Map<String, Map<Integer, Integer>> documentFrequencies) {
		if(documentFrequencies.containsKey(token) && documentFrequencies.get(token).containsKey(documentId)) {
			return documentFrequencies.get(token).get(documentId);
		}
		
		return 0;
	}
}
