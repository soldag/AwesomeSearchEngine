package documents;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.index.IndexReader;
import io.index.IndexWriter;
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
	 * Contains the byte offset in the source file. 
	 */
	private final int offset;
	
	/**
	 * Contains the byte length in the source file.
	 */
	private final int length;
	
	/**
	 * Contains number of tokens in the different parts of this document.
	 */
	private Map<ContentType, Integer> tokenCounts;
	
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param fileId
	 * @param offset
	 * @param length
	 */
	public PatentDocument(int id, int fileId, int offset, int length) {
		this(id, fileId, offset, length, new HashMap<ContentType, Integer>());
	}
	
	/**
	 * Creates a new PatentDocument instance.
	 * @param id
	 * @param fileId
	 * @param offset
	 * @param length
	 * @param tokensCounts
	 */
	public PatentDocument(int id, int fileId, int offset, int length, Map<ContentType, Integer> tokenCounts) {
		this.id = id;
		this.fileId = fileId;
		this.offset = offset;
		this.length = length;
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
	 * Gets the byte offset in the source file.
	 * @return
	 */
	public int getOffset() {
		return this.offset;
	}
	
	/**
	 * Gets the byte length in the source file.
	 * @return
	 */
	public int getLength() {
		return this.length;
	}

	/**
	 * Gets the number of all tokens in this document. Tokens of the document have to be counted first.
	 * @return
	 * @throws IllegalStateException
	 */
	public int getTotalTokensCount() throws IllegalStateException {
		if(this.tokenCounts.isEmpty()) {
			throw new IllegalStateException("Document was not tokenized, yet.");
		}
		
		return this.tokenCounts.values().stream().mapToInt(count -> count.intValue()).sum();
	}
	
	public Map<ContentType, Integer> getTokensCount() {
		return this.tokenCounts;
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
	public static PatentDocument load(IndexReader reader) throws IOException {
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
	public static PatentDocument load(int id, IndexReader reader) throws IOException {
		// Read file id
		int fileId = reader.readInt();
		
		// Read offset and length
		int offset = reader.readInt();
		int length = reader.readInt();
		
		// Read token counts for all content type
		Map<ContentType, Integer> tokenCounts = new HashMap<ContentType, Integer>();
		for(ContentType contentType: ContentType.orderedValues()) {
			// Read token count
			int tokenCount = reader.readInt();
			tokenCounts.put(contentType, tokenCount);
		}
		
		return new PatentDocument(id, fileId, offset, length, tokenCounts);
	}
	
	/**
	 * Saves a document to file using the given file writer.
	 * @param writer
	 * @throws IOException
	 */
	public void save(IndexWriter writer) throws IOException {
		this.save(writer, false);
	}
	
	/**
	 * Saves the body(without id) of a document to file using the given file writer.
	 * @param writer
	 * @param bodyOnly
	 * @throws IOException
	 */
	public void save(IndexWriter writer, boolean bodyOnly) throws IOException {
		if(!bodyOnly) {
			// Write document id
			writer.writeInt(this.getId());
			
			// Start skipping area for properties of the document
			writer.startSkippingArea();
		}
		
		// Write file id
		writer.writeInt(this.getFileId());
		
		// Write offset and length
		writer.writeInt(this.getOffset());
		writer.writeInt(this.getLength());
		
		// Write token counts for all content type
		for(ContentType contentType: ContentType.orderedValues()) {
			// Write token count
			writer.writeInt(this.getTokensCount(contentType));
		}
		
		if(!bodyOnly) {
			// End skipping area for properties of the document
			writer.endSkippingArea();
		}
	}
}
