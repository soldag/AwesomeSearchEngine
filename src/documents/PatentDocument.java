package documents;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import io.FileReader;
import io.FileWriter;
import postings.ContentType;

public class PatentDocument {
	
	/**
	 * Contains the number of digits of a file id. 
	 */
	public static final int FILE_ID_LENGTH = 6;

	/**
	 * Contains the id of the document.
	 */
	private final int id;
	
	/**
	 * Contains the id of the file containing this document.
	 */
	private final int fileId;

	/**
	 * Contains offset and length of the different parts of this document.
	 */
	private final Map<ContentType, Pair<Long, Integer>> contentOffsets;
	
	/**
	 * Contains number of tokens in the different parts of this document.
	 */
	private Map<ContentType, Integer> tokenCounts;
	
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param fileId
	 * @param contentOffsets
	 */
	public PatentDocument(int id, int fileId, Map<ContentType, Pair<Long, Integer>> contentOffsets) {
		this(id, fileId, contentOffsets, new HashMap<ContentType, Integer>());
	}
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param fileId
	 * @param tokensCount
	 * @param contentOffsets
	 * @param tokensCounts
	 */
	public PatentDocument(int id, int fileId, Map<ContentType, Pair<Long, Integer>> contentOffsets, Map<ContentType, Integer> tokenCounts) {
		this.id = id;
		this.fileId = fileId;
		this.contentOffsets = contentOffsets;
		this.tokenCounts = tokenCounts;
	}
	
	
	/**
	 * Gets the id of the document.
	 * @return
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * Gets the id of the source file containing this document.
	 * @return
	 */
	public int getFileId() {
		return this.fileId;
	}

	/**
	 * Gets the number of all tokens in this document. Tokens of the document have to be counted first.
	 * @return
	 * @throws IllegalStateException
	 */
	public int getTokensCount() throws IllegalStateException {
		if(this.tokenCounts.isEmpty()) {
			throw new IllegalStateException("Document was not tokenized, yet.");
		}
		
		return this.tokenCounts.values().stream().mapToInt(count -> count.intValue()).sum();
	}
	
	/**
	 * Gets the number of tokens in a specific part of this document. Tokens of the document have to be counted first.
	 * @param contentType
	 * @return
	 */
	public int getTokensCount(ContentType contentType) {
		if(this.tokenCounts.isEmpty()) {
			throw new IllegalStateException("Document was not tokenized, yet.");
		}
		
		if(this.tokenCounts.containsKey(contentType)) {
			return this.tokenCounts.get(contentType);
		}
		
		return 0;
	}
	
	/**
	 * Sets the number of tokens in the different parts of this document.
	 * @param tokensCount
	 */
	public void setTokensCount(Map<ContentType, Integer> tokenCounts) {
		this.tokenCounts = tokenCounts;
	}
	
	/**
	 * Gets offset and length of the different content types.
	 * @return
	 */
	public Map<ContentType, Pair<Long, Integer>> getContentOffsets() {
		return this.contentOffsets;
	}
	
	/**
	 * Gets the offset and length of a specific content type.
	 * @param type
	 * @return
	 */
	public Pair<Long, Integer> getContentOffset(ContentType type) {
		if(this.getContentOffsets().containsKey(type)) {
			return this.getContentOffsets().get(type);
		}
		
		return null;
	}
	
	/**
	 * Gets a boolean, that determines, whether the document contains content of a specific type.
	 * @param contentType
	 * @return
	 */
	public boolean hasContent(ContentType contentType) {
		return this.getContentOffsets().containsKey(contentType);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PatentDocument) {
			PatentDocument document = (PatentDocument)obj;
			return this.getId() == document.getId();
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return Integer.toString(this.getId());
	}
	
	
	/**
	 * Loads a document from the document map using the given file reader. 
	 * The file descriptor has to be right before the document id.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static PatentDocument load(FileReader reader) throws IOException {
		int id = reader.readInt();
		reader.readInt();
		
		return PatentDocument.load(id, reader);
	}
	
	/**
	 * Loads a document from the document map using the given file reader. 
	 * The file descriptor has to be right after the document id and the length of the properties.
	 * @param id
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static PatentDocument load(int id, FileReader reader) throws IOException {
		// Read file id
		int fileId = reader.readInt();
		
		// Read token counts and offsets for all content type
		Map<ContentType, Integer> tokenCounts = new HashMap<ContentType, Integer>();
		Map<ContentType, Pair<Long, Integer>> contentOffsets = new HashMap<ContentType, Pair<Long, Integer>>();
		for(ContentType contentType: ContentType.orderedValues()) {
			// Read token count
			int tokenCount = reader.readInt();
			tokenCounts.put(contentType, tokenCount);
			
			// Read offset
			long offset = reader.readLong();
			int length = reader.readInt();
			if(offset >= 0 && length > 0) {
				contentOffsets.put(contentType, Pair.of(offset, length));
			}
		}
		
		return new PatentDocument(id, fileId, contentOffsets, tokenCounts);
	}
	
	/**
	 * Saves a document to file using the given file writer.
	 * @param writer
	 * @throws IOException
	 */
	public void save(FileWriter writer) throws IOException {
		// Write document id
		writer.writeInt(this.getId());
		
		// Start skipping area for properties of the document
		writer.startSkippingArea();
		
		// Write file id
		writer.writeInt(this.getFileId());
		
		// Write token counts and offsets for all content type
		for(ContentType contentType: ContentType.orderedValues()) {
			// Write token count
			writer.writeInt(this.getTokensCount(contentType));
			
			// Write offsets
			Pair<Long, Integer> offset = this.getContentOffset(contentType);
			if(offset == null) {
				offset = Pair.of(0l, 0);
			}

			writer.writeLong(offset.getLeft());
			writer.writeInt(offset.getRight());
		}
		
		// End skipping area for properties of the document
		writer.endSkippingArea();
	}
}
