package indexing.contents;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import documents.PatentContentDocument;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import indexing.generic.GenericIndexConstructor;
import io.index.IndexWriter;
import postings.ContentType;

public class ContentsIndexConstuctor extends GenericIndexConstructor<Integer> {
	
	/**
	 * Contains the actual contents index, which stores contents for each document represented by its id.
	 */
	private TIntObjectMap<EnumMap<ContentType, String>> contentsIndex = new TIntObjectHashMap<EnumMap<ContentType, String>>();

	
	/**
	 * Creates a new ContentsIndexConstuctor instance, that does not creat a seek list.
	 * @param compress
	 */
	public ContentsIndexConstuctor(boolean compress) {
		super(compress);
	}
	
	/**
	 * Creates a new ContentsIndexConstuctor instance, that creates a seek list.
	 * @param compress
	 * @param seekList
	 */
	public ContentsIndexConstuctor(boolean compress, ContentsIndexSeekList seekList) {
		super(compress, seekList);
	}
	

	/**
	 * Adds specified contents of the given document to the index.
	 * @param document
	 * @param contentTypesToStore
	 */
	public void put(PatentContentDocument document, ContentType[] contentTypesToStore) {
		EnumMap<ContentType, String> contents = new EnumMap<ContentType, String>(ContentType.class);
		for(ContentType contentType: contentTypesToStore) {
			contents.put(contentType, document.getContent(contentType));
		}
		
		this.contentsIndex.put(document.getId(), contents);
	}
	
	@Override
	public Set<Integer> keys() {
		return IntStream.of(this.contentsIndex.keys()).boxed().collect(Collectors.toSet());
	}

	@Override
	protected void writeEntry(Integer documentId, IndexWriter indexWriter) throws IOException {
		// Write document id
		indexWriter.writeInt(documentId);
		
		// Write contents
		indexWriter.startSkippingArea();
		EnumMap<ContentType, String> contents = this.contentsIndex.get(documentId);
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
	public int size() {
		return this.contentsIndex.size();
	}

	@Override
	public int entriesCount() {
		return this.contentsIndex.size();
	}
	
	@Override
	public void clear() {
		super.clear();
		this.contentsIndex = new TIntObjectHashMap<EnumMap<ContentType, String>>();
	}
}
