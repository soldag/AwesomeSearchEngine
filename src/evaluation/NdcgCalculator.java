package evaluation;

import java.util.ArrayList;
import java.util.stream.IntStream;


public class NdcgCalculator {
	
	/**
	 * Contains a pattern matching the document id in within a formatted result entry of a document.
	 */
	private static final int DOCUMENT_ID_LENGTH = 7;
	
	
	/**
	 * Calculate normalized discounted cumulative gain for actual ranking and a given gold ranking for the very same query at a given rank.
	 * @param goldRanking
	 * @param results
	 * @param rank
	 * @return
	 */
	public double calculate(ArrayList<String> goldRanking, ArrayList<String> results, int rank) {
		// Calculate relevance gains for actual and gold ranking
		double[] goldGains = IntStream.rangeClosed(1, goldRanking.size())
								.mapToDouble(this::calculateGain)
								.toArray();
		double[] actualGains = results.stream()
								.map(result -> result.substring(0, DOCUMENT_ID_LENGTH))
								.mapToInt(documentId -> goldRanking.indexOf(documentId) + 1)
								.mapToDouble(this::calculateGain)
								.toArray();
		
		// Calculate DCG values
		double actualDCG = this.calculateDCG(actualGains, rank);
		double goldDCG = this.calculateDCG(goldGains, rank);
		
		return actualDCG / goldDCG;
	}
	
	/**
	 * Calculate discounted cumulative gain for a given rank.
	 * @param gains
	 * @param rank
	 * @return
	 */
	private double calculateDCG(double[] gains, int rank) {
		return gains[0] + IntStream.rangeClosed(2, rank)
										.mapToDouble(i -> gains[i - 1] / (Math.log(i) / Math.log(2)))
										.sum();
	}

	/**
	 * Calculate graded relevance of a document with a specific rank.
	 * @param rank
	 * @return
	 */
	private double calculateGain(int rank) {
		if(rank <= 0) {
			return 0;
		}
		return 1 + Math.floor(10 * Math.pow(0.5, 0.1d * rank));
	}
}
