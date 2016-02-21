package postings.positions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gnu.trove.list.TIntList;
import io.index.IndexReader;
import io.index.IndexWriter;
import postings.ContentType;

public class LazyPositionMap implements PositionMap {
	
	private IndexReader positionalIndexReader;
	
	private long positionsOffset;
	
	private Map<ContentType, Integer> positionCounts;
	
	private PositionMap positionMap = null;
	
	
	/**
	 * Creates a new LazyPositionMap instance.
	 * @param positionalIndexReader
	 * @param positionsOffset
	 * @param positionCounts
	 */
	private LazyPositionMap(IndexReader positionalIndexReader, long positionsOffset, Map<ContentType, Integer> positionCounts) {
		this.positionalIndexReader = positionalIndexReader;
		this.positionsOffset = positionsOffset;
		this.positionCounts = positionCounts;
	}
	

	@Override
	public Set<ContentType> contentTypeSet() {
		return positionCounts.keySet();
	}

	@Override
	public boolean containsContentType(ContentType contentType) {
		return this.positionCounts.get(contentType) > 0;
	}

	@Override
	public int[] ofContentType(ContentType contentType) {
		return this.getPositionMap().ofContentType(contentType);
	}

	@Override
	public Map<ContentType, TIntList> positions() {
		return this.getPositionMap().positions();
	}

	@Override
	public void put(ContentType contentType, int position) {
		this.getPositionMap().put(contentType, position);
	}

	@Override
	public void put(ContentType contentType, int[] positions) {
		this.getPositionMap().put(contentType, positions);
	}

	@Override
	public void putAll(PositionMap positionMap) {
		if(positionMap instanceof LazyPositionMap) {
			for (Map.Entry<ContentType, Integer> entry: ((LazyPositionMap)positionMap).positionCounts.entrySet()) {
				int count = this.positionCounts.get(entry.getKey()) + entry.getValue();
				this.positionCounts.put(entry.getKey(), count);
			}
		}
		else {
			this.getPositionMap().putAll(positionMap);
		}
	}

	@Override
	public int size() {
		return this.positionCounts.values().stream()
					.mapToInt(Integer::intValue)
					.sum();
	}

	@Override
	public int size(ContentType contentType) {		
		Integer count = this.positionCounts.get(contentType);
		if(count != null) {
			return count;
		}
		
		return 0;
	}
	
	
	/**
	 * Gets the lazy initialized position map loaded from index.
	 * @return
	 */
	private PositionMap getPositionMap() {
		if(this.positionMap == null) {
			try {
				this.positionalIndexReader.seek(this.positionsOffset);
				this.positionMap = EagerPositionMap.load(this.positionalIndexReader, this.positionCounts);
			} catch (IOException e) {
				this.positionMap = null;
			}
		}
		
		return this.positionMap;
	}
	
	
	/**
	 * Loads position information necessary for lazy initialization from given file reader. 
	 * @param frequencyIndexReader
	 * @param positionalIndexReader
	 * @return
	 * @throws IOException
	 */
	public static LazyPositionMap load(IndexReader frequencyIndexReader, IndexReader positionalIndexReader) throws IOException {		
		// Load number of positions per content type
		Map<ContentType, Integer> positionCounts = new HashMap<ContentType, Integer>();
		for(ContentType contentType: ContentType.orderedValues()) {
			int count = frequencyIndexReader.readInt();
			positionCounts.put(contentType, count);
		}
		
		// Get offset for corresponding positions in positional index.
		long positionsOffset = frequencyIndexReader.readLong();
		
		return new LazyPositionMap(positionalIndexReader, positionsOffset, positionCounts);
	}

	@Override
	public void save(IndexWriter frequencyIndexWriter, IndexWriter positionalIndexWriter) throws IOException {
		this.getPositionMap().save(frequencyIndexWriter, positionalIndexWriter);
	}
}
