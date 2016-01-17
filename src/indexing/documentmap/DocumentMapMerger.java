package indexing.documentmap;

import java.io.IOException;

import documents.PatentDocument;
import indexing.generic.GenericIndexMerger;
import indexing.generic.GenericSeekList;
import io.index.IndexReader;
import io.index.IndexWriter;

public class DocumentMapMerger extends GenericIndexMerger<Integer, PatentDocument> {	

	/**
	 * Creates a new DocumentMapMerger instance.
	 * @param isCompressed
	 */
	public DocumentMapMerger(boolean isCompressed) {
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
	protected PatentDocument readValue(Integer documentId, IndexReader indexReader) throws IOException {
		indexReader.getSkippingAreaLenght();
		return PatentDocument.load(documentId, indexReader);
	}

	@Override
	protected void writeValue(PatentDocument document, IndexWriter indexWriter) throws IOException {
		document.save(indexWriter, true);
	}

	@Override
	protected PatentDocument mergeValues(PatentDocument value1, PatentDocument value2) {
		// Not necessary
		return null;
	}

	@Override
	protected GenericSeekList<Integer> newSeeklist() {
		return new DocumentMapSeekList();
	}
}
