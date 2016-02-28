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
		for(Map.Entry<ContentType, TIntList> entry: positionMap.positions().entrySet()) {
			if(this.positions.containsKey(entry.getKey())) {
				this.positions.get(entry.getKey()).addAll(entry.getValue());
			}
			else {
				this.positions.put(entry.getKey(), entry.getValue());
			}
		}
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
	 * Loads positions from given file readers. File pointer has to be at the beginning of the position map.
	 * @param frequencyIndexReader
	 * @param positionalIndexReader
	 * @return
	 * @throws IOException
	 */
	public static EagerPositionMap load(IndexReader frequencyIndexReader, IndexReader positionalIndexReader) throws IOException {
		// Read numbers of positions per content type
		Map<ContentType, Integer> positionCounts = new HashMap<ContentType, Integer>();
		for(ContentType contentType: ContentType.orderedValues()) {
			int count = frequencyIndexReader.readInt();
			if(count > 0) {
				positionCounts.put(contentType, count);
			}
		}
		
		// Get offset for corresponding positions in positional index and read those positions
		long positionsOffset = frequencyIndexReader.readLong();
		positionalIndexReader.seek(positionsOffset);
		
		return EagerPositionMap.load(positionalIndexReader.getSkippingAreaReader(), positionCounts);
	}
	/**
	 * Loads positions from given file reader. File pointer has to be directly before the positions.
	 * @param positionalIndexReader
	 * @param positionCounts
	 * @return
	 * @throws IOException
	 */
	public static EagerPositionMap load(IndexReader positionalIndexReader, Map<ContentType, Integer> positionCounts) throws IOException {
		// Read single positions
		EagerPositionMap positionMap = new EagerPositionMap();
		for(ContentType contentType: ContentType.orderedValues()) {
			if(positionCounts.containsKey(contentType)) {
				int lastPosition = 0;
				int count = positionCounts.get(contentType);
				for(int i = 0; i < count; i++) {
					int position = positionalIndexReader.readInt();
					if(positionalIndexReader.isCompressed()) {
						position += lastPosition;
						lastPosition = position;
					}
					positionMap.put(contentType, position);
				}
			}
		}
		
		return positionMap;
	}
	
	@Override
	public void save(IndexWriter frequencyIndexWriter, IndexWriter positionalIndexWriter) throws IOException {
		// Write numbers of positions per content type to frequency index
		for(ContentType contentType: ContentType.orderedValues()) {
			int count = 0;
			if(this.containsContentType(contentType)) {
				count = this.size(contentType);
			}
			frequencyIndexWriter.writeInt(count);
		}
		
		// Write single positions to positional index and pointer to frequency index
		frequencyIndexWriter.writeLong(positionalIndexWriter.getFilePointer());
		for(ContentType contentType: ContentType.orderedValues()) {
			int[] positions = this.ofContentType(contentType);
			for(int i = 0; i < positions.length; i++) {
				int position = positions[i];
				if(positionalIndexWriter.isCompressed() && i > 0) {
					position -= positions[i - 1];
				}
				positionalIndexWriter.writeInt(position);
			}
		}
	}
}
