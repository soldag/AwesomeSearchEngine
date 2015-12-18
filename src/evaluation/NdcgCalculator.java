package evaluation;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


public class NdcgCalculator {
	
	/**
	 * Contains a pattern matching the patent title in within a formatted result entry of a document.
	 */
	private static final Pattern RESULT_TITLE_PATTERN = Pattern.compile("\033\\[1m(?<title>.*)\033\\[0m");	
	
	
	/**
	 * Calculate normalized discounted cumulative gain for actual ranking and a given gold ranking for the very same query at a given rank.
	 * @param goldRanking
	 * @param ranking
	 * @param rank
	 * @return
	 */
	public double calculate(ArrayList<String> goldRanking, ArrayList<String> ranking, int rank) {
		// Get title of patent at the specified position of our ranking
		String title = this.extractTitle(ranking.get(rank - 1));
		
		// Get rank of the same patent in the gold ranking
		int goldRank = goldRanking.indexOf(title);
		if(goldRank < 0) {
			return 0d;
		}
		
		// Calculate DCG values
		double actualDCG = this.calculateDCG(rank);
		double goldDCG = this.calculateDCG(goldRank);
		
		return actualDCG / goldDCG;
	}
	
	/**
	 * Extracts the patent title of a given patent result (including document id, title and snippet)
	 * @param result
	 * @return
	 */
	private String extractTitle(String result) {
		Matcher matcher = RESULT_TITLE_PATTERN.matcher(result);
		if(matcher.find()) {
			return matcher.group("title");
		}
		
		return null;
	}
	
	/**
	 * Calculate discounted cumulative gain for a given rank.
	 * @param rank
	 * @return
	 */
	private double calculateDCG(int rank) {
		return this.calculateGain(1) + IntStream.rangeClosed(2, rank)
										.mapToDouble(i -> this.calculateGain(i) / (Math.log(i) / Math.log(2)))
										.sum();
	}

	/**
	 * Calculate graded relevance of a document with a specific rank.
	 * @param rank
	 * @return
	 */
	private double calculateGain(int rank) {
		return 1 + Math.floor(10 * Math.pow(0.5, 0.1d * rank));
	}
}
