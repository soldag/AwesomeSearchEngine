package indexing.invertedindex;

import java.io.IOException;

import indexing.generic.GenericIndexMerger;
import indexing.generic.GenericSeekList;
import io.index.IndexReader;
import io.index.IndexWriter;
import postings.TokenPostings;

public class InvertedIndexMerger extends GenericIndexMerger<String, TokenPostings> {
	
	/**
	 * Creates a new InvertedIndexMerger instance.
	 * @param isCompressed
	 */
	public InvertedIndexMerger(boolean isCompressed) {
		super(isCompressed);
	}


	@Override
	protected String readKey(IndexReader indexReader) throws IOException {
		return indexReader.readString();
	}


	@Override
	protected void writeKey(String token, IndexWriter indexWriter) throws IOException {
		indexWriter.writeString(token);
	}


	@Override
	protected TokenPostings readValue(String token, IndexReader indexReader) throws IOException {
		return TokenPostings.load(indexReader, true);
	}


	@Override
	protected void writeValue(TokenPostings postings, IndexWriter indexWriter) throws IOException {
		postings.save(indexWriter);
	}


	@Override
	protected TokenPostings mergeValues(TokenPostings postings1, TokenPostings postings2) {
		postings1.putAll(postings2);
		return postings1;
	}


	@Override
	protected GenericSeekList<String> newSeeklist() {
		return new InvertedIndexSeekList();
	}
	
	
	
}
