package postings;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DocumentPostings {
	
	/**
	 * Contains the postings for a specific document.
	 */
	private final Map<String, PositionMap> postings;
	
	
	/**
	 * Creates a new DocumentPostings instance.
	 * @param positions
	 */
	public DocumentPostings(Map<String, PositionMap> positions) {
		this.postings = positions;
	}
		
	
	/**
	 * Gets the set of all tokens contained in the postings.
	 * @return
	 */
	public Set<String> tokenSet() {
		return this.postings.keySet();
	}
	
	/**
	 * Returns all positions of the postings.
	 * @return
	 */
	public Collection<PositionMap> positions() {
		return this.postings.values();
	}
	
	/**
	 * Returns all tokens with their corresponding positions.
	 * @return
	 */
	public Set<Map.Entry<String, PositionMap>> entrySet() {
		return this.postings.entrySet();
	}
	
	
	/**
	 * Determines, whether there are postings for the given token.
	 * @param token
	 * @return
	 */
	public boolean containsToken(String token) {
		return this.postings.containsKey(token);
	}
	
	
	/**
	 *  Gets the positions for a specific token.
	 * @param token
	 * @return
	 */
	public PositionMap ofToken(String token) {
		return this.postings.get(token);
	}
	
	/**
	 * Adds a new posting for the document.
	 * @param token
	 * @param positions
	 */
	public void put(String token, PositionMap positions) {
		this.postings.put(token, positions);
	}
	
	
	@Override
	public String toString() {
		return this.postings.toString();
	}
}
