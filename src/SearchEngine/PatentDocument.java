package SearchEngine;

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
	 * Contains the number of tokens in this document.
	 */
	private int tokensCount = -1;
	
	/**
	 * Contains the list of the most frequent words in the document.
	 */
	private List<String> mostFrequentTokens = null;
	
	/**
	 * Contains the title of the document.
	 */
	private final String title;
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param tokensCount
	 * @param mostFrequentTokens
	 * @param title
	 */
	public PatentDocument(int id, int tokensCount, List<String> mostFrequentTokens, String title) {
		this.id = id;
		this.tokensCount = tokensCount;
		this.mostFrequentTokens = mostFrequentTokens;
		this.title = title;
	}
	
	
	/**
	 * Gets the id of the document.
	 * @return
	 */
	public int getId() {
		return this.id;
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
	 * Gets the title of the document.
	 * @return
	 */
	public String getTitle() {
		return this.title;
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
		
		// Read tokens count
		int tokensCount = buffer.getInt();
		
		// Read title
		int titleLength = buffer.getInt();
		byte[] titleBytes = new byte[titleLength];
		buffer.get(titleBytes);
		String title = new String(titleBytes, ENCODING);
		
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
		
		return new PatentDocument(id, tokensCount, mostFrequentTokens, title);
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
		byte[] titleBytes = this.getTitle().getBytes(ENCODING);
		int mostFrequentTermLength = termsBytes.stream().mapToInt(x -> x.length).sum() + termsBytes.size() * Integer.BYTES + Integer.BYTES; // Encoded most frequent tokens + number of total tokens + length of each token 
		int propertiesLength = 2 * Integer.BYTES + titleBytes.length + mostFrequentTermLength; // Tokens count + title length + title bytes + most frequent term entries
		int capacity = 2 * Integer.BYTES + propertiesLength; // Document id + properties length (skip pointer) + properties bytes
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		
		// Write document id
		buffer.putInt(this.getId());
		
		// Write properties length
		buffer.putInt(propertiesLength);
		
		// Write tokens count
		buffer.putInt(tokensCount);
		
		// Write title
		buffer.putInt(titleBytes.length);
		buffer.put(titleBytes);
		
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
		return String.valueOf(String.format("%s %s", this.getId(), this.getTitle()));
	}
}
