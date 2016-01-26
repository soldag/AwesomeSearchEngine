package documents;

import java.util.Map;
import postings.ContentType;

public class PatentContentDocument extends PatentDocument {

	/**
	 * Contains the contents per document part.
	 */	
	private final Map<ContentType, String> contents;
	
	/**
	 * Contains ids of linked documents
	 */
	private final int[] linkedDocumentIds;
	
	
	/**
	 * Creates a new PatentContentDocument instance.
	 * @param document
	 * @param contents
	 * @param linkedDocumentIds
	 */
	public PatentContentDocument(PatentDocument document, Map<ContentType, String> contents, int[] linkedDocumentIds) {
		this(document.getId(), document.getFileId(), document.getOffset(), document.getLength(), contents, linkedDocumentIds);
	}
	
	/**
	 * Creates a new PatentContentDocument instance.
	 * @param documentId
	 * @param fileId
	 * @param offset
	 * @param length
	 * @param contents
	 * @param linkedDocumentIds
	 */
	public PatentContentDocument(int documentId, int fileId, int offset, int length, Map<ContentType, String> contents, int[] linkedDocumentIds) {
		super(documentId, fileId, offset, length);
		
		this.contents = contents;
		this.linkedDocumentIds = linkedDocumentIds;
	}
	
	
	/**
	 * Gets the contents per document part.
	 * @return
	 */
	public Map<ContentType, String> getContents() {
		return this.contents;
	}
	
	/**
	 * Gets the content of a specific document part.
	 * @param documentPart
	 * @return
	 */
	public String getContent(ContentType documentPart) {
		if(this.getContents().containsKey(documentPart)) {
			return this.getContents().get(documentPart);
		}
		
		return null;
	}
	
	/**
	 * Gets ids of linked documents.
	 * @return
	 */
	public int[] getLinkedDocumentIds() {
		return this.linkedDocumentIds;
	}
	
	/**
	 * Returns a clone of the current PatentDocument without content to save memory consumption.
	 * @return
	 */
	public PatentDocument withoutContent() {
		return new PatentDocument(this.getId(), this.getFileId(), this.getOffset(), this.getLength(), this.getTokensCount());
	}
}
