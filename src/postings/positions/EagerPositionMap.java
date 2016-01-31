package postings.positions;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import io.index.IndexReader;
import io.index.IndexWriter;
import postings.ContentType;

public class EagerPositionMap implements PositionMap {

	/**
	 * Contains multiple positions for different content types.
	 */
	private EnumMap<ContentType, TIntList> positions = new EnumMap<ContentType, TIntList>(ContentType.class);
	
	
	@Override
	public Set<ContentType> contentTypeSet() {
		return this.positions.keySet();
	}
	

	@Override
	public boolean containsContentType(ContentType contentType) {
		return this.positions.containsKey(contentType);
	}
	
	
	@Override
	public EnumMap<ContentType, TIntList> positions() {
		return this.positions;
	}

	@Override
	public int[] ofContentType(ContentType contentType) {
		TIntList documentIds = this.positions.get(contentType);
		if(documentIds != null) {
			documentIds.sort();
			return documentIds.toArray();
		}
		
		return new int[0];
	}
	

	@Override
	public void put(ContentType contentType, int position) {
		this.positions.putIfAbsent(contentType, new TIntArrayList());
		this.positions.get(contentType).add(position);
	}

	@Override
	public void put(ContentType contentType, int[] positions) {
		this.positions.put(contentType, new TIntArrayList(positions));
	}

	@Override
	public void putAll(PositionMap positionMap) {
		this.positions.putAll(positionMap.positions());
	}
	
	
	@Override
	public int size() {
		return this.positions.size();
	}

	@Override
	public int size(ContentType contentType) {
		TIntList positions = this.positions.get(contentType);
		if(positions != null) {
			return positions.size();
		}
		
		return 0;
	}
	
	
	@Override
	public String toString() {
		return this.positions.toString();
	}
	
	
	/**
	 * Loads positions from given file reader. File pointer has to be at the beginning of the position map.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static EagerPositionMap load(IndexReader indexReader) throws IOException {
		// Read numbers of positions per content type
		Map<ContentType, Integer> positionCounts = new HashMap<ContentType, Integer>();
		for(ContentType contentType: ContentType.orderedValues()) {
			int count = indexReader.readInt();
			positionCounts.put(contentType, count);
		}
		
		return EagerPositionMap.load(indexReader, positionCounts);
	}
	/**
	 * Loads positions from given file reader. File pointer has to be directly after the position counts.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static EagerPositionMap load(IndexReader indexReader, Map<ContentType, Integer> positionCounts) throws IOException {
		// Skip skipping area length
		indexReader.getSkippingAreaLength();
		
		// Read single positions
		EagerPositionMap positionMap = new EagerPositionMap();
		for(ContentType contentType: ContentType.orderedValues()) {
			int lastPosition = 0;
			int count = positionCounts.get(contentType);
			for(int i = 0; i < count; i++) {
				int position = indexReader.readInt();
				if(indexReader.isCompressed()) {
					position += lastPosition;
					lastPosition = position;
				}
				positionMap.put(contentType, position);
			}
		}
		
		return positionMap;
	}
	
	@Override
	public void save(IndexWriter indexWriter) throws IOException {
		// Write numbers of positions per content type
		for(ContentType contentType: ContentType.orderedValues()) {
			int count = 0;
			if(this.containsContentType(contentType)) {
				count = this.size(contentType);
			}
			indexWriter.writeInt(count);
		}
		
		// Write single positions
		indexWriter.startSkippingArea();
		for(ContentType contentType: ContentType.orderedValues()) {
			int[] positions = this.ofContentType(contentType);
			for(int i = 0; i < positions.length; i++) {
				int position = positions[i];
				if(indexWriter.isCompressed() && i > 0) {
					position -= positions[i - 1];
				}
				indexWriter.writeInt(position);
			}
		}
		indexWriter.endSkippingArea();
	}
}
