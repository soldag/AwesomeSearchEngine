package postings;

import java.util.Arrays;

public enum ContentType {
	Title("us-bibliographic-data-grant/invention-title", 0.7),
	Abstract("abstract", 0.3);
	
	
	/**
	 * Contains the XPath to XML elements containing the specific document part.
	 */
	private final String xPath;
	
	/**
	 * Contains the weighting factor for the specific document part.
	 */
	private final double weightingFactor;
	
	/**
	 * Creates a new TokenOrigin.
	 * @param xPath
	 * @param weightingFactor
	 */
	ContentType(String xPath, double weightingFactor) {
		this.xPath = xPath;
		this.weightingFactor = weightingFactor;
	}
	
	/**
	 * Gets the XPath to XML elements containing the specific document part.
	 * @return
	 */
	public String getXPath() {
		return this.xPath;
	}
	
	/**
	 * Gets the weighting factor for the specific document part.
	 * @return
	 */
	public double getWeightingFactor() {
		return this.weightingFactor;
	}
	
	
	/**
	 * Contains an array containing the constants of this enum type ordered by their ordinal value.
	 */
	private static ContentType[] orderedValues;
	
	/**
	 * Returns an array containing the constants of this enum type ordered by their ordinal value.
	 * @return
	 */
	public static ContentType[] orderedValues() {
		if(ContentType.orderedValues == null) {
			ContentType.orderedValues = Arrays.stream(ContentType.values())
											.sorted((x,y) -> Integer.compare(x.ordinal(), y.ordinal()))
											.toArray(ContentType[]::new);
		}
		
		return ContentType.orderedValues;
	}
}
