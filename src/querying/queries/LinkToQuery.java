package querying.queries;


public class LinkToQuery implements Query {
	
	/**
	 * Contains the type of the query.
	 */
	public static final String TYPE = "LINK";
	
	/**
	 * Contains the id of document, whose citations are wanted.
	 */
	private final int documentId;
	
	
	/**
	 * Creates a new LinkToQuery instance.
	 * @param documentId
	 */
	public LinkToQuery(int documentId) {
		this.documentId = documentId;
	}

	
	/**
	 * Gets the id of document, whose citations are wanted.
	 * @return
	 */
	public int getDocumentId() {
		return this.documentId;
	}

	@Override
	public String getType() {
		return TYPE;
	}
}
