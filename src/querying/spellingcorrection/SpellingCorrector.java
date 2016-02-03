package querying.spellingcorrection;

import java.io.IOException;

import gnu.trove.map.TObjectIntMap;
import indexing.invertedindex.InvertedIndexReader;

public class SpellingCorrector {
	
	/**
	 * Contains a edit distance calculator.
	 */
	private DamerauLevenshteinCalculator damerauLevenshtein;

	/**
	 * Contains the inverted index readers.
	 */
	private InvertedIndexReader indexReader;
	
	
	/**
	 * Creates a new SpellingCorrector instance.
	 * @param damerauLevenshtein
	 * @param indexReader
	 */
	public SpellingCorrector(DamerauLevenshteinCalculator damerauLevenshtein, InvertedIndexReader indexReader) {
		this.damerauLevenshtein = damerauLevenshtein;
		this.indexReader = indexReader;
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
		TObjectIntMap<String> tokenCandidates = this.indexReader.getTokens(startCharacter);

		// Get the candidate with the lowest edit distance
		MostSimilarTokenProcedure mostSimilarTokenProcedure = new MostSimilarTokenProcedure(this.damerauLevenshtein);
		tokenCandidates.forEachEntry(mostSimilarTokenProcedure);
		
		return mostSimilarTokenProcedure.getMostSimilarToken();
	}
}
