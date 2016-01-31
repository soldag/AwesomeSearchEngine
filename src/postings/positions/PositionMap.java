package postings.positions;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import gnu.trove.list.TIntList;
import io.index.IndexWriter;
import postings.ContentType;

public interface PositionMap {
	
	/**
	 * Gets the set of all content types stored in this map.
	 * @return
	 */
	public Set<ContentType> contentTypeSet();
	
	
	/**
	 * Determines, whether the current map contains positions of the given content type.
	 * @param contentType
	 * @return
	 */
	public boolean containsContentType(ContentType contentType);
	
	
	/**
	 * Gets the positions of a given content type.
	 * @param contentType
	 * @return
	 */
	public int[] ofContentType(ContentType contentType);
	
	/**
	 * Gets all positions mapped to the corresponding content type.
	 * @return
	 */
	public Map<ContentType, TIntList> positions();
	
	
	/**
	 * Adds a single position of specific content type.
	 * @param contentType
	 * @param position
	 */
	public void put(ContentType contentType, int position);
	
	/**
	 * Adds multiple positions of specific content type.
	 * @param contentType
	 * @param positions
	 */
	public void put(ContentType contentType, int[] positions);
	
	/**
	 * Merges a given PositionMap instance into the current one.
	 * @param positionMap
	 */
	public void putAll(PositionMap positionMap);
	
	
	/**
	 * Gets the total number of stored positions.
	 * @return
	 */
	public int size();
	
	/**
	 * Gets the number of stored positions of a certain content type.
	 * @param contentType
	 * @return
	 */
	public int size(ContentType contentType);
	
	/**
	 * Writes current position map to given index writer.
	 * @param writer
	 * @return
	 * @throws IOException 
	 */
	public void save(IndexWriter indexWriter) throws IOException;
}
