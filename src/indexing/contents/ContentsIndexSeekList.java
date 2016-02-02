package indexing.contents;

import java.io.IOException;

import indexing.generic.GenericSeekList;
import io.index.IndexReader;
import io.index.IndexWriter;

public class ContentsIndexSeekList extends GenericSeekList<Integer> {
	
	/**
	 * Determines, how many documents are skipped in between two seek list entries.
	 */
	private static final int SKIP_NUMBER = 200;

	/**
	 * Creates a new ContentsIndexSeekList instance.
	 */
	public ContentsIndexSeekList() {
		super(SKIP_NUMBER);
	}

	
	@Override
	protected Integer readKey(IndexReader reader) throws IOException {
		return reader.readInt();
	}

	@Override
	protected void writeKey(Integer documentId, IndexWriter writer) throws IOException {
		writer.writeInt(documentId);
	}

}
