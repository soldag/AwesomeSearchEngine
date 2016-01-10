package indexing.documentmap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import documents.PatentDocument;
import indexing.generic.GenericIndexConstructor;
import io.FileWriter;

public class DocumentMapConstructor extends GenericIndexConstructor<Integer> {

	/**
	 * Contains the documents mapped to their ids.
	 */
	private Map<Integer, PatentDocument> documentMap = new HashMap<Integer, PatentDocument>();
	
	
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
	protected Set<Integer> keys() {
		return this.documentMap.keySet();
	}

	@Override
	protected void writeEntry(Integer key, FileWriter indexWriter) throws IOException {
		PatentDocument document = this.documentMap.get(key);
		document.save(indexWriter);
	}
	
	@Override
	public int size() {
		return this.documentMap.size();
	}
	
	@Override
	public void clear() {
		super.clear();
		this.documentMap.clear();
	}
}
