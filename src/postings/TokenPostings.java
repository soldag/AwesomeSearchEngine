package postings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import documents.PatentDocument;

public class TokenPostings {

	/**
	 * Contains the postings for a specific token.
	 */
	private final Map<Integer, PositionMap> postings;
	
	/**
	 * Contains a lookup map for PatentDocument instances.
	 */
	private final Map<Integer, PatentDocument> documents;
	
	
	/**
	 * Creates a new, empty TokenPostings instance.
	 */
	public TokenPostings() {
		this.postings = new HashMap<Integer, PositionMap>();
		this.documents = new HashMap<Integer, PatentDocument>();
	}	
	
	/**
	 * Creates a new TokenPostings instance.
	 * @param postings
	 * @param documents
	 */
	public TokenPostings(Map<Integer, PositionMap> postings, Map<Integer, PatentDocument> documents) {
		this.postings = postings;
		this.documents = documents;
	}
	
	
	/**
	 * Gets the set of all document ids contained in the postings.
	 * @return
	 */
	public Set<Integer> documentIdSet() {
		return this.postings.keySet();
	}
	
	/**
	 * Gets the set of all document contained in the postings.
	 * @return
	 */
	public Set<PatentDocument> documentSet() {
		return this.documentIdSet().stream()
				.filter(documentId -> this.documents.containsKey(documentId))
				.map(documentId -> this.documents.get(documentId))
				.collect(Collectors.toSet());
	}	
	
	/**
	 * Returns all positions of the postings.
	 * @return
	 */
	public Collection<PositionMap> positions() {
		return this.postings.values();
	}
	
	/**
	 * Returns all document ids with their corresponding positions.
	 * @return
	 */
	public Set<Map.Entry<Integer, PositionMap>> entrySet() {
		return this.postings.entrySet();
	}
	
	
	/**
	 * Gets the positions for a specific document.
	 * @param documentId
	 * @return
	 */
	public PositionMap ofDocument(int documentId) {
		return this.postings.get(documentId);
	}
	
	/**
	 * Gets the positions for a specific document.
	 * @param document
	 * @return
	 */
	public PositionMap ofDocument(PatentDocument document) {
		return this.postings.get(document.getId());
	}
	
	
	/**
	 * Adds a new posting for the token.
	 * @param documentId
	 * @param positions
	 */
	public void put(int documentId, PositionMap positions) {
		if(this.postings.containsKey(documentId)) {
			this.postings.get(documentId).putAll(positions);
		}
		else {
			this.postings.put(documentId, positions);
		}
	}

	/**
	 * Adds a new posting for the token.
	 * @param document
	 * @param positions
	 */
	public void put(PatentDocument document, PositionMap positions) {
		this.put(document.getId(), positions);
		this.putDocument(document);
	}
	
	/**
	 * Adds all given postings for the token.
	 * @param postings
	 */
	public void putAll(TokenPostings postings) {
		this.postings.putAll(postings.postings);
		this.documents.putAll(postings.documents);
	}
	
	/**
	 * Adds a document to the internal mapping.
	 * @param document
	 */
	private void putDocument(PatentDocument document) {
		if(!this.documents.containsKey(document.getId())) {
			this.documents.put(document.getId(), document);
		}
	}
	
	
	@Override
	public String toString() {
		return this.postings.toString();
	}
	
	
	/**
	 * Reads the postings of a specific token from a given file. 
	 * The file descriptor has to be right behind the corresponding token.
	 * @param input
	 * @param isCompressed
	 * @return
	 * @throws IOException
	 */
	public static TokenPostings readFrom(RandomAccessFile input, boolean isCompressed) throws IOException {
		int length = input.readInt();
		return readFrom(input, length, isCompressed);
	}
	
	/**
	 * Reads the postings of a specific token from a given file.
	 * The file descriptor has to be right behind the corresponding token and the length of the postings.
	 * @param input
	 * @param length
	 * @param isCompressed
	 * @return
	 * @throws IOException
	 */
	public static TokenPostings readFrom(RandomAccessFile input, int length, boolean isCompressed) throws IOException {
		TokenPostings postingList = new TokenPostings();
		
		int lastDocumentId = 0;
		long endPosition = input.getFilePointer() + length;
		while(input.getFilePointer() < endPosition) {
			// Read document id
			int documentId = input.readInt();
			if(isCompressed) {
				documentId += lastDocumentId;
				lastDocumentId = documentId;
			}
			
			// Read positions grouped by content type
			PositionMap positionMap = PositionMap.readFrom(input, isCompressed);
			
			postingList.put(documentId, positionMap);
		}
		
		return postingList;
	}
	
	/**
	 * Writes the postings to a given file (including the length of the postings).
	 * @param output
	 * @param compress
	 * @throws IOException
	 */
	public void writeTo(RandomAccessFile output, boolean compress) throws IOException {
		int lastDocumentId = 0;
		ByteArrayOutputStream postingsStream = new ByteArrayOutputStream();
		int[] sortedDocumentIds = this.documentIdSet().stream().mapToInt(x -> x.intValue()).sorted().toArray();
		for(int documentId: sortedDocumentIds) {
			PositionMap positionMap = this.ofDocument(documentId);
			byte[] positionMapBytes = positionMap.writeTo(compress);
			
			// Allocate byte buffer
			int length = Integer.BYTES + positionMapBytes.length;
			ByteBuffer buffer = ByteBuffer.allocate(length);
			
			// Write document id
			if(compress) {
				documentId -= lastDocumentId;
				lastDocumentId = documentId;
			}
			buffer.putInt(documentId);
			
			// Write positions grouped by content type
			buffer.put(positionMapBytes);
			
			postingsStream.write(buffer.array());
		}
		
		// Write postings length and postings
		byte[] postingsBytes = postingsStream.toByteArray();
		output.writeInt(postingsBytes.length);
		output.write(postingsBytes);
	}
}
