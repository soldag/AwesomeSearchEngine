package parsing;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class PatentDocument {

	/**
	 * Contains the id of the document.
	 */
	private final int id;
	
	/**
	 * Contains the id of the file containing this document.
	 */
	private final int fileId;
	
	/**
	 * Contains the file offset of the title tag.
	 */
	private final long titleOffset;
	
	/**
	 * Contains the length of the title tag.
	 */
	private final int titleLength;
	
	/**
	 * Contains the file offset of the abstract tag.
	 */
	private final long abstractOffset;
	
	/**
	 * Contains the length of the abstract tag.
	 */
	private final int abstractLength;
	
	/**
	 * Contains the number of tokens in this document.
	 */
	private int tokensCount = -1;
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param fileId
	 * @param titleOffset
	 * @param titleLength
	 * @param abstractOffset
	 * @param abstractLength
	 * @param tokensCount
	 */
	public PatentDocument(int id, int fileId, long titleOffset, int titleLength, long abstractOffset, int abstractLength, int tokensCount) {
		this.id = id;
		this.fileId = fileId;
		this.titleOffset = titleOffset;
		this.titleLength = titleLength;
		this.abstractOffset = abstractOffset;
		this.abstractLength = abstractLength;
		this.tokensCount = tokensCount;
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
	 * Gets the file offset of the title tag.
	 * @return
	 */
	public long getTitleOffset() {
		return this.titleOffset;
	}
	
	/**
	 * Gets the length of the title tag.
	 * @return
	 */
	public int getTitleLength() {
		return this.titleLength;
	}
	
	/**
	 * Gets the file offset of the abstract tag.
	 * @return
	 */
	public long getAbstractOffset() {
		return this.abstractOffset;
	}
	
	/**
	 * Gets the length of the abstract tag.
	 * @return
	 */
	public int getAbstractLength() {
		return this.abstractLength;
	}

	/**
	 * Gets the number of tokens in this document. Tokens of the document have to be counted first.
	 * @return
	 * @throws IllegalStateException
	 */
	public int getTokensCount() throws IllegalStateException {
		if(this.tokensCount == -1) {
			throw new IllegalStateException("Document was not tokenized, yet.");
		}
		return this.tokensCount;
	}
	
	/**
	 * Sets the number of tokens in this document.
	 * @param tokensCount
	 */
	public void setTokensCount(int tokensCount) {
		this.tokensCount = tokensCount;
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
		
		// Read offsets
		long titleStartOffset = buffer.getLong();
		int titleEndOffset = buffer.getInt();
		long abstractStartOffset = buffer.getLong();
		int abstractEndOffset = buffer.getInt();
		
		// Read tokens count
		int tokensCount = buffer.getInt();
		
		return new PatentDocument(id, fileId, titleStartOffset, titleEndOffset, abstractStartOffset, abstractEndOffset, tokensCount);
	}
	
	/**
	 * Serializes current PatentDocument to byte array.
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public byte[] toBytes() throws UnsupportedEncodingException {
		// Calculate capacity of buffer
		int propertiesLength = 4 * Integer.BYTES + 2 * Long.BYTES; // File id + Tokens count + offsets
		int capacity = 2 * Integer.BYTES + propertiesLength; // Document id + properties length (skip pointer) + properties bytes
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		
		// Write document id
		buffer.putInt(this.getId());
		
		// Write properties length
		buffer.putInt(propertiesLength);
		
		// Write file id
		buffer.putInt(this.getFileId());
		
		// Write offsets
		buffer.putLong(this.getTitleOffset());
		buffer.putInt(this.getTitleLength());
		buffer.putLong(this.getAbstractOffset());
		buffer.putInt(this.getAbstractLength());
		
		// Write tokens count
		buffer.putInt(tokensCount);
		
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
