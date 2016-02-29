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
	 * Contains the original, mispelled token.
	 */
	private String misspelledToken;
	
	
	/**
	 * Define default values for determination of most similar string.
	 */
	private int minDistance = Integer.MAX_VALUE;
	private String minDistanceToken = null;
	private int minDistanceTokenOccurrences = 0;	
	
	
	/**
	 * Creates a new MostSimilarTokenProcedure instance.
	 * @param damerauLevenshtein
	 * @param misspelledToken
	 */
	public MostSimilarTokenProcedure(DamerauLevenshteinCalculator damerauLevenshtein, String misspelledToken) {
		this.damerauLevenshtein = damerauLevenshtein;
		this.misspelledToken = misspelledToken;
	}
	

	@Override
	public boolean execute(String token, int occurrencesCount) {
		if (Math.abs(this.misspelledToken.length() - token.length()) <= MAX_LENGTH_DIFFERENCE) {
			int distance = this.damerauLevenshtein.execute(this.misspelledToken, token);
			
			if(distance > MAX_DISTANCE) {
				return true;
			}
			if(distance < minDistance) {
				// New minimum
				this.minDistance = distance;
				this.minDistanceToken = token;
			}
			else if(distance == minDistance) {
				// Since the edit distances are the same, take number of occurrences in the whole collection into account.
				if(occurrencesCount > minDistanceTokenOccurrences) {
					this.minDistance = distance;
					this.minDistanceToken = token;
				}
			}
		}
		return true;
	}

	
	/**
	 * Gets the result of the computation.
	 * @return
	 */
	public String getMostSimilarToken() {
		return this.minDistanceToken;
	}
}
