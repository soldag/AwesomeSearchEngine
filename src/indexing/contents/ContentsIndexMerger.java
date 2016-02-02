package indexing.contents;

import java.io.IOException;
import java.util.EnumMap;

import indexing.generic.GenericIndexMerger;
import indexing.generic.GenericSeekList;
import io.index.IndexReader;
import io.index.IndexWriter;
import postings.ContentType;

public class ContentsIndexMerger extends GenericIndexMerger<Integer, EnumMap<ContentType, String>> {

	/**
	 * Creates a new ContentsIndexMerger instance.
	 * @param isCompressed
	 */
	public ContentsIndexMerger(boolean isCompressed) {
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
	protected EnumMap<ContentType, String> readValue(Integer documentId, IndexReader indexReader) throws IOException {
		indexReader.getSkippingAreaLength();
		EnumMap<ContentType, String> contents = new EnumMap<ContentType, String>(ContentType.class);
		for(ContentType contentType: ContentType.orderedValues()) {
			String content = indexReader.readString();
			if(content != null && !content.isEmpty()) {
				contents.put(contentType, content);
			}
		}
		
		return contents;
	}

	@Override
	protected void writeValue(EnumMap<ContentType, String> contents, IndexWriter indexWriter) throws IOException {
		indexWriter.startSkippingArea();
		for(ContentType contentType: ContentType.orderedValues()) {
			String content = "";
			if(contents.containsKey(contentType)) {
				content = contents.get(contentType);
			}
			indexWriter.writeString(content);
		}
		indexWriter.endSkippingArea();
	}

	@Override
	protected EnumMap<ContentType, String> mergeValues(EnumMap<ContentType, String> contents1, EnumMap<ContentType, String> contents2) {
		contents1.putAll(contents2);
		return contents1;
	}

	@Override
	protected GenericSeekList<Integer> newSeeklist() {
		return new ContentsIndexSeekList();
	}

}
