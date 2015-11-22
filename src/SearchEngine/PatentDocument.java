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
	private int tokensCount;
	
	/**
	 * Contains the title of the document.
	 */
	private final String title;
	
	/**
	 * TODO: add comment
	 */
	private List<String> mostFrequentTerms;
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param tokensCount
	 * @param title
	 */
	public PatentDocument(int id, int tokensCount, String title) {
		this.id = id;
		this.tokensCount = tokensCount;
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
	 * TODO: add comment
	 */
	public List<String> getMostFrequentTerms() {
		return this.mostFrequentTerms;
	}
	
	/**
	 * TODO: add comment
	 */
	public void setMostFrequentTerms(List<String> mostFrequentTerms) {
		this.mostFrequentTerms = mostFrequentTerms;
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
		int mostFrequentTermsCount = buffer.getInt();
		List<String> mostFrequentTerms = new ArrayList<String>();
		for(int i = 0; i < mostFrequentTermsCount; i++){
			int termLength = buffer.getInt();
			byte[] termBytes = new byte[termLength];
			buffer.get(termBytes);
			String term = new String(termBytes, ENCODING);
			mostFrequentTerms.add(term);
		}
		PatentDocument document = new PatentDocument(id, tokensCount, title);
		document.setMostFrequentTerms(mostFrequentTerms);
		
		return document;
	}
	
	/**
	 * Serializes current PatentDocument to byte array.
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public byte[] toBytes() throws UnsupportedEncodingException {
		List<Byte[]> termsBytes = new ArrayList<Byte[]>();
		for(String term: mostFrequentTerms) {
			termsBytes.add(ArrayUtils.toObject(term.getBytes(ENCODING)));
		}
		
		// Calculate capacity of buffer
		byte[] titleBytes = this.getTitle().getBytes(ENCODING);
		int mostFrequentTermLength = termsBytes.stream().mapToInt(x -> x.length).sum() + termsBytes.size()*Integer.BYTES + Integer.BYTES;
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
		
		// Write mostFrequentTerms
		buffer.putInt(mostFrequentTerms.size());
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
