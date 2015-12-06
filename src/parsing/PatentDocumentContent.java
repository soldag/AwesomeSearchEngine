package parsing;

import org.apache.commons.lang3.tuple.Pair;

public class PatentDocumentContent {

	/**
	 * Contains the value of the content.
	 */
	private final String value;
	
	/**
	 * Contains the position of the content in the source file (consisting of offset and length).
	 */
	private final Pair<Long, Integer> position;
	
	
	/**
	 * Creates a new PatentDocumentPart instance.
	 * @param value
	 * @param position
	 */
	public PatentDocumentContent(String value, Pair<Long, Integer> position) {
		this.value = value;
		this.position = position;
	}

	
	/**
	 * Gets the value of the content.
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Gets the position of the content in the source file (consisting of offset and length).
	 * @return
	 */
	public Pair<Long, Integer> getPosition() {
		return this.position;
	}
}
