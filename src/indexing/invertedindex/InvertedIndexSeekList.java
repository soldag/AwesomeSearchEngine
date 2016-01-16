package indexing.invertedindex;

import java.io.IOException;

import indexing.generic.GenericSeekList;
import io.index.IndexReader;
import io.index.IndexWriter;

public class InvertedIndexSeekList extends GenericSeekList<String> {
	
	/**
	 * Determines, how many documents are skipped in between two seek list entries.
	 */
	private static final int SKIP_NUMBER = 200;
	
	
	/**
	 * Creates a new IndexSeekList instance.
	 */
	public InvertedIndexSeekList() {
		super(SKIP_NUMBER);
	}


	@Override
	protected String readKey(IndexReader reader) throws IOException {
		return reader.readString();
	}


	@Override
	protected void writeKey(String token, IndexWriter writer) throws IOException {
		writer.writeString(token);
	}
}
