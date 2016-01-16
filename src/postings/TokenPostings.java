package postings;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import documents.PatentDocument;
import io.index.IndexReader;
import io.index.IndexWriter;

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
	
	
	/**
	 * Gets the number of all occurrences of the tokens.
	 * @return
	 */
	public long totalOccurencesCount() {
		return this.positions().stream()
					.mapToLong(positions -> positions.size())
					.sum();
	}
	
	
	@Override
	public String toString() {
		return this.postings.toString();
	}

	
	/**
	 * Loads postings for a specific token from a given file reader. 
	 * The file descriptor has to be right after the token.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static TokenPostings load(IndexReader reader) throws IOException {
		int length = reader.readInt();
		return TokenPostings.load(reader, length);
	}
	
	/**
	 * Loads postings for a specific token from a given file reader. 
	 * The file descriptor has to be right afterthe length of the postings.
	 * @param reader
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public static TokenPostings load(IndexReader reader, int length) throws IOException {
		TokenPostings postings = new TokenPostings();
		
		int lastDocumentId = 0;
		long endPosition = reader.getFilePointer() + length;
		while(reader.getFilePointer() < endPosition) {
			// Read document id
			int documentId = reader.readInt();
			if(reader.isCompressed()) {
				documentId += lastDocumentId;
				lastDocumentId = documentId;
			}
			
			// Read positions grouped by content type
			PositionMap positionMap = PositionMap.load(reader);
			
			postings.put(documentId, positionMap);
		}
		
		return postings;
	}
	
	/**
	 * Saves the postings using the given file writer.
	 * @param writer
	 * @throws IOException
	 */
	public void save(IndexWriter writer) throws IOException {
		int lastDocumentId = 0;
		int[] sortedDocumentIds = this.documentIdSet().stream().mapToInt(x -> x.intValue()).sorted().toArray();
		for(int documentId: sortedDocumentIds) {
			PositionMap positionMap = this.ofDocument(documentId);
			
			// Write document id
			if(writer.isCompressed()) {
				int originalDocumentId = documentId;
				documentId -= lastDocumentId;
				lastDocumentId = originalDocumentId;
			}
			writer.writeInt(documentId);
			
			// Write positions
			positionMap.save(writer);
		}
	}
}
