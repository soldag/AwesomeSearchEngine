package indexing.citations;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import indexing.generic.GenericIndexMerger;
import indexing.generic.GenericSeekList;
import io.index.IndexReader;
import io.index.IndexWriter;

public class CitationIndexMerger extends GenericIndexMerger<Integer, Set<Integer>> {

	/**
	 * Creates a new CitationIndexMerger instance.
	 * @param isCompressed
	 */
	public CitationIndexMerger(boolean isCompressed) {
		super(isCompressed);
	}
	

	@Override
	protected Integer readKey(IndexReader indexReader) throws IOException {
		return indexReader.readInt();
	}

	@Override
	protected void writeKey(Integer documentId, IndexWriter indexWriter) throws IOException {
		indexWriter.writeInt(documentId);
	}

	@Override
	protected Set<Integer> readValue(Integer documentId, IndexReader indexReader) throws IOException {
		indexReader.getSkippingAreaLength();
		int count = indexReader.readInt();
		
		int lastDocumentId = 0;
		Set<Integer> documentIds = new HashSet<Integer>(count);
		for(int i = 0; i < count; i++) {
			int linkingDocumentId = indexReader.readInt();
			if(indexReader.isCompressed()) {
				documentId += lastDocumentId;
				lastDocumentId = documentId;
			}
			documentIds.add(linkingDocumentId);
		}
		
		return documentIds;
	}

	@Override
	protected void writeValue(Set<Integer> documentIds, IndexWriter indexWriter) throws IOException {
		// Start skipping area
		indexWriter.startSkippingArea();
		
		// Write referencing document ids
		indexWriter.writeInt(documentIds.size());
		
		int lastDocumentId = 0;
		documentIds = documentIds.stream().sorted().collect(Collectors.toSet());
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
	protected Set<Integer> mergeValues(Set<Integer> value1, Set<Integer> value2) {
		return Sets.union(value1, value2);
	}

	@Override
	protected GenericSeekList<Integer> newSeeklist() {
		return new CitationIndexSeekList();
	}

}
