package postings;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.Ints;

public class PositionMap {

	/**
	 * Contains multiple positions for different content types.
	 */
	private Multimap<ContentType, Integer> positions = TreeMultimap.create();
	
	
	/**
	 * Gets the set of all content types stored in this map.
	 * @return
	 */
	public Set<ContentType> contentTypeSet() {
		return this.positions.keySet();
	}
	
	
	/**
	 * Determines, whether the current map contains positions of the given content type.
	 * @param contentType
	 * @return
	 */
	public boolean containsContentType(ContentType contentType) {
		return this.positions.containsKey(contentType);
	}
	
	
	/**
	 * Gets the positions of a given content type.
	 * @param contentType
	 * @return
	 */
	public int[] ofContentType(ContentType contentType) {
		return this.positions.get(contentType).stream()
					.mapToInt(x -> x.intValue())
					.toArray();
	}
	
	
	/**
	 * Adds a single position of specific content type.
	 * @param contentType
	 * @param position
	 */
	public void put(ContentType contentType, int position) {
		this.positions.put(contentType, position);
	}
	
	/**
	 * Adds multiple positions of specific content type.
	 * @param contentType
	 * @param positions
	 */
	public void put(ContentType contentType, int[] positions) {
		this.positions.putAll(contentType, Ints.asList(positions));
	}
	
	/**
	 * Merges a given PositionMap instance into the current one.
	 * @param positionMap
	 */
	public void putAll(PositionMap positionMap) {
		this.positions.putAll(positionMap.positions);
	}
	
	
	/**
	 * Gets the total number of stored positions.
	 * @return
	 */
	public int size() {
		return this.positions.size();
	}
	
	@Override
	public String toString() {
		return this.positions.toString();
	}
	
	
	/**
	 * Reads position map from given file reader. 
	 * @param input
	 * @param compressed
	 * @return
	 * @throws IOException
	 */
	public static PositionMap readFrom(DataInput input, boolean compressed) throws IOException {
		PositionMap positionMap = new PositionMap();
		for(ContentType contentType: ContentType.orderedValues()) {
			int lastPosition = 0;
			int positionsLength = input.readInt();
			for(int i = 0; i < positionsLength; i++) {
				int position = input.readInt();
				if(compressed) {
					position += lastPosition;
					lastPosition = position;
				}
				positionMap.put(contentType, position);
			}
		}
		
		return positionMap;
	}
	
	/**
	 * Writes current position map to given file writer.
	 * @param compress
	 * @return
	 */
	public byte[] writeTo(boolean compress) {
		// Allocate byte buffer
		int length = (ContentType.values().length + this.size()) * Integer.BYTES;
		ByteBuffer buffer = ByteBuffer.allocate(length);
		
		for(ContentType contentType: ContentType.orderedValues()) {
			if(!this.containsContentType(contentType)) {
				// If document does not contain this type, set number of the corresponding positions to 0
				buffer.putInt(0);
			}
			else {
				// Write number of corresponding positions
				int[] positions = this.ofContentType(contentType);
				buffer.putInt(positions.length);
				
				// Write single positions
				for(int i = 0; i < positions.length; i++) {
					int position = positions[i];
					if(compress && i > 0) {
						position -= positions[i - 1];
					}
					buffer.putInt(position);
				}
			}
		}
		
		return buffer.array();
	}
}
