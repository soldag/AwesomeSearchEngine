package SearchEngine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Posting implements Comparable<Posting> {

	/**
	 * Contains the id of the document, in which the token occurs.
	 */
	private final int documentId;
	
	/**
	 * Contains positions of the token in the document.
	 */
	private final int[] positions;
	
	
	/**
	 * Creates a new Posting instance with multiple positions.
	 * @param documentId
	 * @param positions
	 */
	public Posting(int documentId, int[] positions) {
		this.documentId = documentId;
		this.positions = positions;
	}
	
	/**
	 * Creates a new Posting instance with a single position.
	 * @param documentId
	 * @param positions
	 */
	public Posting(int documentId, int position) {
		this(documentId, new int[] { position });
	}
	
	
	/**
	 * Gets the id of the document, in which the token occurs.
	 * @return
	 */
	public int getDocumentId() {
		return this.documentId;
	}
	
	/**
	 * Gets the positions of the token in the document.
	 * @return
	 */
	public int[] getPositions() {
		return this.positions;
	}

	@Override
	public int compareTo(Posting posting) {
		return this.documentId - posting.documentId;
	}
	
	/**
	 * Creates a list of Posting instances by deserializing given byte array representing a posting list.
	 * @param postingsBytes
	 * @param isCompressed
	 * @return
	 */
	public static List<Posting> fromBytes(byte[] postingsBytes, boolean isCompressed) {
		ByteBuffer buffer = ByteBuffer.wrap(postingsBytes);
		
		int lastDocumentId = 0;
		List<Posting> postings = new ArrayList<Posting>();
		while(buffer.position() < buffer.limit()) {
			// Read document ID
			int documentId = buffer.getInt();
			if(isCompressed) {
				documentId += lastDocumentId;
				lastDocumentId = documentId;
			}
			
			// Read positions
			int frequency = buffer.getInt();
			int[] positions = new int[frequency];
			for(int j = 0; j < frequency; j++) {
				positions[j] = buffer.getInt();
				if(isCompressed && j > 0) {
					positions[j] += positions[j-1];
				}
			}
			
			postings.add(new Posting(documentId, positions));
		}
		
		return postings;
	}
	
	/**
	 * Serializes current Posting to byte array.
	 * @param compress
	 * @param lastDocumentId
	 * @return
	 */
	public byte[] toBytes(boolean compress, int lastDocumentId) {
		// Allocate byte buffer
		int capacity = (2 + this.positions.length) * Integer.BYTES;
		ByteBuffer buffer = ByteBuffer.allocate(capacity);
		
		// Write documentId
		int documentId = this.getDocumentId();
		if(compress) {
			documentId -= lastDocumentId;
		}
		buffer.putInt(documentId);
		
		// Write sorted positions
		int[] positions = this.getPositions();
		Arrays.sort(positions);
		buffer.putInt(positions.length);
		for(int i = 0; i < positions.length; i++) {
			int position = positions[i];
			if(compress && i > 0) {
				position = positions[i-1];
			}
			buffer.putInt(position);
		}
		
		return buffer.array();
	}
	
	@Override
	public String toString() {
		return String.format("(%s, %s)", this.getDocumentId(), Arrays.toString(this.getPositions()));
	}
}
