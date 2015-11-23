package querying;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import SearchEngine.Posting;
import indexing.InvertedIndexReader;
import indexing.InvertedIndexSeekList;

public class SpellingCorrector {

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
	 * Contain index readers.
	 */
	private InvertedIndexReader indexReader;
	private InvertedIndexSeekList indexSeekList;
	
	
	/**
	 * Creates a new SpellingCorrector instance.
	 * @param damerauLevenshtein
	 * @param indexReader
	 * @param seekList
	 */
	public SpellingCorrector(DamerauLevenshteinCalculator damerauLevenshtein, InvertedIndexReader indexReader, InvertedIndexSeekList seekList) {
		this.damerauLevenshtein = damerauLevenshtein;
		this.indexReader = indexReader;
		this.indexSeekList = seekList;
	}

	
	/**
	 * Corrects a misspelled token using the inverted index. If no correction can be found, null is returned.
	 * @param token
	 * @return Corrected token or null, if no correction can be found.
	 * @throws IOException 
	 */
	public String correctToken(String misspelledToken) throws IOException {		
		// Get candidates for corrected tokens, that start with the same character as the misspelled one
		String startCharacter = misspelledToken.substring(0,1);
		int startOffset = this.indexSeekList.getIndexOffset(startCharacter);
		Map<String, List<Posting>> tokens = this.indexReader.getPostingsMap(startCharacter, startOffset, true);

		// Get the candidate with the lowest edit distance
		int minimumDistance = Integer.MAX_VALUE;
		String minimumDistanceToken = null;
		for(String token: tokens.keySet()){
			if (Math.abs(token.length() - misspelledToken.length()) <= MAX_LENGTH_DIFFERENCE) {
				int distance = this.damerauLevenshtein.execute(misspelledToken, token);
				
				if(distance > MAX_DISTANCE) {
					continue;
				}
				if(distance < minimumDistance) {
					// New minimum
					minimumDistance = distance;
					minimumDistanceToken = token;
				}
				else if(distance == minimumDistance) {
					// Since the edit distances are the same, take number of occurrences in the whole collection into account.
					int minimumTokenCount = this.getCollectionFrequency(minimumDistanceToken, tokens);
					int tokenCount = this.getCollectionFrequency(token, tokens);			
					if(tokenCount > minimumTokenCount) {
						minimumDistance = distance;
						minimumDistanceToken = token;
					}
				}
			}
		}
		
		return minimumDistanceToken;
	}
	
	/**
	 * Returns the number of occurrences in the whole collection for a given token.
	 * @param token
	 * @param tokens
	 * @return int
	 */
	private int getCollectionFrequency(String token, Map<String, List<Posting>> tokens) {
		return tokens.get(token).stream().mapToInt(x -> x.getPositions().length).sum();
	}
}
