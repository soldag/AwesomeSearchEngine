package indexing.citations;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import indexing.generic.GenericIndexConstructor;
import io.index.IndexWriter;

public class CitationIndexConstructor extends GenericIndexConstructor<Integer> {
	
	/**
	 * Contains the actual citation index.
	 */
	private Multimap<Integer, Integer> citationIndex = HashMultimap.<Integer, Integer>create();
	

	/**
	 * Creates a new CitationIndexConstructor instance, that does not create a seek list.
	 * @param compress
	 */
	public CitationIndexConstructor(boolean compress) {
		super(compress);
	}
	
	/**
	 * Creates a new CitationIndexConstructor instance, that creates a seek list.
	 * @param compress
	 * @param seekList
	 */
	public CitationIndexConstructor(boolean compress, CitationIndexSeekList seekList) {
		super(compress, seekList);
	}
	
	
	/**
	 * Adds a new entry to the index.
	 * @param documentId
	 * @param linkingDocument
	 */
	public void add(int documentId, int linkingDocument) {
		this.citationIndex.put(documentId, linkingDocument);
	}
	

	@Override
	public Set<Integer> keys() {
		return citationIndex.keySet();
	}

	@Override
	protected void writeEntry(Integer key, IndexWriter indexWriter) throws IOException {
		// Write document id
		indexWriter.writeInt(key);
		
		// Start skipping area
		indexWriter.startSkippingArea();
		
		// Write referencing document ids
		Set<Integer> documentIds = this.citationIndex.get(key).stream().sorted().collect(Collectors.toSet());
		indexWriter.writeInt(documentIds.size());
		
		int lastDocumentId = 0;
		for(int documentId: documentIds) {
			if(indexWriter.isCompressed()) {
				int originalDocumentId = documentId;
				documentId -= lastDocumentId;
				lastDocumentId = originalDocumentId;
			}
			indexWriter.writeInt(documentId);
		}
		
		// End skipping area
		indexWriter.endSkippingArea();
	}

	@Override
	public int size() {
		return this.citationIndex.values().size();
	}

	@Override
	public int entriesCount() {
		return this.citationIndex.size();
	}

}
