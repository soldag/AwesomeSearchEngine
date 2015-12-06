package documents;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import parsing.PatentDocumentContent;
import postings.ContentType;

public class PatentContentDocument extends PatentDocument {

	/**
	 * Contains the contents per document part.
	 */	
	private final Map<ContentType, String> contents;
	
	
	/**
	 * Creates a new PatentContentDocument instance.
	 * @param documentId
	 * @param fileId
	 * @param contentOffsets
	 * @param contents
	 */
	public PatentContentDocument(int documentId, int fileId, Map<ContentType, PatentDocumentContent> contents) {
		super(documentId, fileId, extractContentPositions(contents));
		
		this.contents = contents.entrySet().stream().collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue().getValue()));
	}
	
	/**
	 * Extracts a map containing only the positions of each content type.
	 * @param contents
	 * @return
	 */
	private static Map<ContentType, Pair<Long, Integer>> extractContentPositions(Map<ContentType, PatentDocumentContent> contents) {
		return contents.entrySet().stream().collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue().getPosition()));
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
}
