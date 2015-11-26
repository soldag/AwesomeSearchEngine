package SearchEngine;

public class PatentContentDocument extends PatentDocument {

	/**
	 * Contains the title of the document.
	 */
	private final String title;
	
	/**
	 * Contains the abstract of the document.
	 */
	private final String abstractText;
	
	
	/**
	 * Creates a new PatentContentDocument instance.
	 * @param documentId
	 * @param fileId
	 * @param titleOffset
	 * @param titleLength
	 * @param abstractOffset
	 * @param abstractLength
	 * @param title
	 * @param abstractText
	 */
	public PatentContentDocument(int documentId, int fileId, long titleOffset, int titleLength, long abstractOffset, int abstractLength, String title, String abstractText) {
		super(documentId, fileId, titleOffset, titleLength, abstractOffset, abstractLength, -1, null);
		
		this.title = title;
		this.abstractText = abstractText;
	}
	
	
	/**
	 * Gets the title of the document.
	 * @return
	 */
	public String getTitle() {
		return this.title;
	}
	
	/**
	 * Gets the abstract of the document.
	 * @return
	 */
	public String getAbstractText() {
		return this.abstractText;
	}
}
