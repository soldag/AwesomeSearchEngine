package indexing.documentmap;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import documents.PatentDocument;
import gnu.trove.map.hash.TIntObjectHashMap;
import indexing.generic.GenericIndexConstructor;
import io.index.IndexWriter;

public class DocumentMapConstructor extends GenericIndexConstructor<Integer> {

	/**
	 * Contains the documents mapped to their ids.
	 */
	
	private TIntObjectHashMap<PatentDocument> documentMap = new TIntObjectHashMap<PatentDocument>();
	
	
	/**
	 * Creates a new DocumentMapConstructor instance, that does not create a seek list.
	 * @param compress
	 * @throws IOException
	 */
	public DocumentMapConstructor(boolean compress) throws IOException {
		super(compress);
	}
	
	/**
	 * Creates a new DocumentMapConstructor instance, that creates a seek list.
	 * @param compress
	 * @param seekList
	 * @throws IOException
	 */
	public DocumentMapConstructor(boolean compress, DocumentMapSeekList seekList) {
		super(compress, seekList);
	}
	
	
	/**
	 * Adds a document to the map.
	 * @param document
	 * @throws IOException
	 */
	public void add(PatentDocument document) {
		this.documentMap.put(document.getId(), document);
	}

	@Override
	public Set<Integer> keys() {
		return IntStream.of(this.documentMap.keys()).boxed().collect(Collectors.toSet());
	}

	@Override
	protected void writeEntry(Integer key, IndexWriter indexWriter) throws IOException {
		PatentDocument document = this.documentMap.get(key);
		document.save(indexWriter);
	}
	
	@Override
	public long size() {
		return this.documentMap.size();
	}
	
	@Override
	public int entriesCount() {
		return this.documentMap.size();
	}
	
	@Override
	public void clear() {
		super.clear();
		this.documentMap = new TIntObjectHashMap<PatentDocument>();
	}
}
