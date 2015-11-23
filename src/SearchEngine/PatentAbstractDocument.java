package SearchEngine;

public class PatentAbstractDocument extends PatentDocument {

	/**
	 * Contains the abstract of the document.
	 */
	private final String abstractText;
	
	
	/**
	 * Creates a new PatentAbstractDocument instance.
	 * @param documentId
	 * @param title
	 * @param abstractText
	 */
	public PatentAbstractDocument(int documentId, String title, String abstractText) {
		super(documentId, -1, null, title);
		
		this.abstractText = abstractText;
	}
	
	/**
	 * Gets the abstract of the document.
	 * @return
	 */
	public String getAbstractText() {
		return this.abstractText;
	}
}
