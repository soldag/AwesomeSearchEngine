package parsing.parsers;

public class PatentDocumentProperty {

	/**
	 * Contains the value of the property.
	 */
	private final String value;
	
	/**
	 * Contains the offset of the corresponding XML start element.
	 */
	private final long offset;
	
	/**
	 * Contains the length of the corresponding XML element.
	 */
	private final int length;
	
	
	/**
	 * Creates a new PatentDocumentProperty instance.
	 * @param value
	 * @param offset
	 * @param length
	 */
	public PatentDocumentProperty(String value, long offset, int length) {
		this.value = value;
		this.offset = offset;
		this.length = length;
	}

	
	/**
	 * Gets the value of the property.
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Gets the offset of the corresponding XML start element.
	 * @return
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * Gets the length of the corresponding XML element.
	 * @return
	 */
	public int getLength() {
		return length;
	}
}
