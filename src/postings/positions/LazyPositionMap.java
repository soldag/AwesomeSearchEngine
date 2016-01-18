package postings.positions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;

import io.index.IndexReader;
import io.index.IndexWriter;
import postings.ContentType;

public class LazyPositionMap implements PositionMap {
	
	private IndexReader indexReader;
	
	private int offset;
	
	private Map<ContentType, Integer> positionCounts;
	
	private PositionMap positionMap = null;
	
	
	/**
	 * Creates a new LazyPositionMap instance.
	 * @param indexReader
	 * @param offset
	 * @param positionCounts
	 */
	private LazyPositionMap(IndexReader indexReader, int offset, Map<ContentType, Integer> positionCounts) {
		this.indexReader = indexReader;
		this.offset = offset;
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
	public Multimap<ContentType, Integer> positions() {
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
				this.indexReader.seek(this.offset);
				this.positionMap = EagerPositionMap.load(this.indexReader, this.positionCounts);
			} catch (IOException e) {
				this.positionMap = null;
			}
		}
		
		return this.positionMap;
	}
	
	
	/**
	 * Loads position information necessary for lazy initialization from given file reader. 
	 * @param indexReader
	 * @return
	 * @throws IOException
	 */
	public static LazyPositionMap load(IndexReader indexReader) throws IOException {		
		// Load number of positions per content type
		Map<ContentType, Integer> positionCounts = new HashMap<ContentType, Integer>();
		for(ContentType contentType: ContentType.orderedValues()) {
			int count = indexReader.readInt();
			positionCounts.put(contentType, count);
		}
		
		// Get offset
		int offset = (int)indexReader.getFilePointer();
		
		// Skip positions
		indexReader.skipSkippingArea();
		
		return new LazyPositionMap(indexReader, offset, positionCounts);
	}

	@Override
	public void save(IndexWriter indexWriter) throws IOException {
		this.getPositionMap().save(indexWriter);
	}
}
