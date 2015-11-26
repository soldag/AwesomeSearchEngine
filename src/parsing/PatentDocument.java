package parsing;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class PatentDocument {
	
	/**
	 * Determines the encoding for writing text of the document to file.
	 */
	private static final String ENCODING = "UTF-8";

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
	 * Contains the list of the most frequent words in the document.
	 */
	private List<String> mostFrequentTokens = null;
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param fileId
	 * @param titleOffset
	 * @param titleLength
	 * @param abstractOffset
	 * @param abstractLength
	 * @param tokensCount
	 * @param mostFrequentTokens
	 */
	public PatentDocument(int id, int fileId, long titleOffset, int titleLength, long abstractOffset, int abstractLength, int tokensCount, List<String> mostFrequentTokens) {
		this.id = id;
		this.fileId = fileId;
		this.titleOffset = titleOffset;
		this.titleLength = titleLength;
		this.abstractOffset = abstractOffset;
		this.abstractLength = abstractLength;
		this.tokensCount = tokensCount;
		this.mostFrequentTokens = mostFrequentTokens;
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
	 * Gets the list of the most frequent words in the document.
	 */
	public List<String> getMostFrequentTokens() {
		if(this.mostFrequentTokens == null) {
			throw new IllegalStateException("Document was not tokenized, yet.");
		}
		return this.mostFrequentTokens;
	}
	
	/**
	 * Sets the list of the most frequent words in the document.
	 * @param mostFrequentTokens
	 */
	public void setMostFrequentTokens(List<String> mostFrequentTokens) {
		this.mostFrequentTokens = mostFrequentTokens;
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
		
		// Read most frequent terms
		int mostFrequentTokensCount = buffer.getInt();
		List<String> mostFrequentTokens = new ArrayList<String>();
		for(int i = 0; i < mostFrequentTokensCount; i++){
			int tokenLength = buffer.getInt();
			byte[] tokenBytes = new byte[tokenLength];
			buffer.get(tokenBytes);
			String token = new String(tokenBytes, ENCODING);
			
			mostFrequentTokens.add(token);
		}
		
		return new PatentDocument(id, fileId, titleStartOffset, titleEndOffset, abstractStartOffset, abstractEndOffset, tokensCount, mostFrequentTokens);
	}
	
	/**
	 * Serializes current PatentDocument to byte array.
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public byte[] toBytes() throws UnsupportedEncodingException {
		List<Byte[]> termsBytes = new ArrayList<Byte[]>();
		for(String term: mostFrequentTokens) {
			termsBytes.add(ArrayUtils.toObject(term.getBytes(ENCODING)));
		}
		
		// Calculate capacity of buffer
		int mostFrequentTermLength = termsBytes.stream().mapToInt(x -> x.length).sum() + termsBytes.size() * Integer.BYTES + Integer.BYTES; // Encoded most frequent tokens + number of total tokens + length of each token 
		int propertiesLength = 4 * Integer.BYTES + 2 * Long.BYTES + mostFrequentTermLength; // File id + Tokens count + offsets + most frequent term entries
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
		
		// Write most frequent tokens
		buffer.putInt(mostFrequentTokens.size());
		for(Byte[] term: termsBytes) {
			buffer.putInt(term.length);
			buffer.put(ArrayUtils.toPrimitive(term));
		}
		
		return buffer.array();
	}
	
	@Override
	public String toString() {
		return Integer.toString(this.getId());
	}
}
