package documents;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

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
	
	
	/**
	 * Creates a new PatentDocument instance by deserializing given byte array representing the whole document.
	 * @param patentDocumentBytes
	 * @return PatentDocument
	 * @throws UnsupportedEncodingException
	 */
	public static PatentDocument fromBytes(byte[] patentDocumentBytes) throws UnsupportedEncodingException {
		ByteBuffer buffer = ByteBuffer.wrap(patentDocumentBytes);
		
		int id = buffer.getInt();
		int propertiesLength = buffer.getInt();
		byte[] propertyBytes = new byte[propertiesLength];
		System.arraycopy(patentDocumentBytes, buffer.position(), propertyBytes, 0, propertiesLength);
		
		return PatentDocument.fromPropertyBytes(id, propertyBytes);
	}
	
	/**
	 * Creates a new PatentDocument instance by deserializing given byte array representing only the properties.
	 * @param id
	 * @param propertyBytes
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static PatentDocument fromPropertyBytes(int id, byte[] propertyBytes) throws UnsupportedEncodingException {
		ByteBuffer buffer = ByteBuffer.wrap(propertyBytes);

		// Read file id
		int fileId = buffer.getInt();
		
		// Read token counts and offsets for all content type
		Map<ContentType, Integer> tokenCounts = new HashMap<ContentType, Integer>();
		Map<ContentType, Pair<Long, Integer>> contentOffsets = new HashMap<ContentType, Pair<Long, Integer>>();
		for(ContentType contentType: ContentType.orderedValues()) {
			// Read token count
			int tokenCount = buffer.getInt();
			tokenCounts.put(contentType, tokenCount);
			
			// Read offset
			long offset = buffer.getLong();
			int length = buffer.getInt();
			if(offset >= 0 && length > 0) {
				contentOffsets.put(contentType, Pair.of(offset, length));
			}
		}
		
		return new PatentDocument(id, fileId, contentOffsets, tokenCounts);
	}
	
	/**
	 * Serializes current PatentDocument to byte array.
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public byte[] toBytes() throws UnsupportedEncodingException {
		// Calculate capacity of buffer
		int tokenCountsLength = ContentType.values().length * Integer.BYTES; // token count per content type
		int offsetsLength = ContentType.values().length * (Integer.BYTES + Long.BYTES); // offset + length per content type
		int propertiesLength = Integer.BYTES + tokenCountsLength + offsetsLength; // File id + tokens count + offsets
		int capacity = 2 * Integer.BYTES + propertiesLength; // Document id + properties length (skip pointer) + properties bytes
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		
		// Write document id
		buffer.putInt(this.getId());
		
		// Write properties length
		buffer.putInt(propertiesLength);
		
		// Write file id
		buffer.putInt(this.getFileId());
		
		// Write token counts and offsets for all content type
		for(ContentType contentType: ContentType.orderedValues()) {
			// Write token count
			buffer.putInt(this.getTokensCount(contentType));
			
			// Write offsets
			Pair<Long, Integer> offset = this.getContentOffset(contentType);
			if(offset == null) {
				offset = Pair.of(0l, 0);
			}

			buffer.putLong(offset.getLeft());
			buffer.putInt(offset.getRight());
		}
		
		return buffer.array();
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
}
