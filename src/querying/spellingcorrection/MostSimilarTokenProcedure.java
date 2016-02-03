package querying.spellingcorrection;

import gnu.trove.procedure.TObjectIntProcedure;

public class MostSimilarTokenProcedure implements TObjectIntProcedure<String> {
	
	/**
	 * Determines, how much the length of the misspelled and possibly correct token can differ.
	 */
	private static final int MAX_LENGTH_DIFFERENCE = 2;
	
	/**
	 * Determines the limit of the edit distance. 
	 */
	private static final int MAX_DISTANCE = 3;
	
	
	/**
	 * Contains a edit distance calculator.
	 */
	private DamerauLevenshteinCalculator damerauLevenshtein;
	
	
	/**
	 * Define default values for determination of most similar string.
	 */
	private int minDistance = Integer.MAX_VALUE;
	private String minDistanceToken = null;
	private int minDistanceTokenOccurrences = 0;	
	
	
	/**
	 * Creates a new MostSimilarTokenProcedure instance.
	 * @param damerauLevenshtein
	 */
	public MostSimilarTokenProcedure(DamerauLevenshteinCalculator damerauLevenshtein) {
		this.damerauLevenshtein = damerauLevenshtein;
	}
	

	@Override
	public boolean execute(String token, int occurrencesCount) {
		if (Math.abs(token.length() - token.length()) <= MAX_LENGTH_DIFFERENCE) {
			int distance = this.damerauLevenshtein.execute(token, token);
			
			if(distance > MAX_DISTANCE) {
				return false;
			}
			if(distance < minDistance) {
				// New minimum
				minDistance = distance;
				minDistanceToken = token;
			}
			else if(distance == minDistance) {
				// Since the edit distances are the same, take number of occurrences in the whole collection into account.
				if(occurrencesCount > minDistanceTokenOccurrences) {
					minDistance = distance;
					minDistanceToken = token;
				}
			}
		}
		return false;
	}

	
	/**
	 * Gets the result of the computation.
	 * @return
	 */
	public String getMostSimilarToken() {
		return this.minDistanceToken;
	}
}
