package indexing.invertedindex;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import indexing.generic.GenericIndexConstructor;
import io.FileReaderWriterFactory;
import io.index.IndexWriter;
import postings.ContentType;
import postings.PostingTable;
import postings.TokenPostings;

public class InvertedIndexConstructor extends GenericIndexConstructor<String> {
	
	/**
	 * Contains the actual inverted index.
	 */
	private PostingTable invertedIndex = new PostingTable();
	
	private IndexWriter positionalIndexWriter;
	
	
	/**
	 * Creates a new InvertedIndexConstructor instance, that does not create a seek list.
	 * @param compress
	 */
	public InvertedIndexConstructor(boolean compress) {
		super(compress);
	}
	
	/**
	 * Creates a new InvertedIndexConstructor instance, that creates a seek list.
	 * @param compress
	 * @param seekList
	 */
	public InvertedIndexConstructor(boolean compress, InvertedIndexSeekList seekList) {
		super(compress, seekList);
	}
	
	
	/**
	 * Adds a new posting to index.
	 * @param documentId
	 * @param token
	 * @param contentType
	 * @param position
	 */
	public void add(int documentId, String token, ContentType contentType, int position) {
		this.invertedIndex.put(token, documentId, contentType, position);
	}

	@Override
	public Set<String> keys() {
		return this.invertedIndex.tokenSet();
	}
	
	/**
	 * Writes frequency index and positional index to file.
	 * @param frequencyIndexFile
	 * @param positionalIndexFile
	 * @throws IOException
	 */
	public void save(File frequencyIndexFile, File positionalIndexFile) throws IOException {
		this.saveWithSeekList(frequencyIndexFile, positionalIndexFile, null);
	}
	
	/**
	 * Writes frequency index, positional index and seek list to file.
	 * @param frequencyIndexFile
	 * @param positionalIndexFile
	 * @param seekListFile
	 * @throws IOException
	 */
	public void saveWithSeekList(File frequencyIndexFile, File positionalIndexFile, File seekListFile) throws IOException {
		this.positionalIndexWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(positionalIndexFile, this.isCompressed());
		this.saveWithSeekList(frequencyIndexFile, seekListFile);
		
		this.positionalIndexWriter.close();
		this.positionalIndexWriter = null;
	}

	@Override
	protected void writeEntry(String key, IndexWriter indexWriter) throws IOException {
		// Write token
		indexWriter.writeString(key);
		
		// Write postings
		TokenPostings postings = this.invertedIndex.ofToken(key);
		indexWriter.startSkippingArea();
		postings.save(indexWriter, this.positionalIndexWriter);
		indexWriter.endSkippingArea();
	}
	
	@Override
	public int size() {
		return this.invertedIndex.totalTokenOccurencesCount();
	}
	
	@Override
	public int entriesCount() {
		return this.invertedIndex.size();
	}
	
	/**
	 * Deletes all entries of the inverted index.
	 */
	@Override
	public void clear() {
		super.clear();
		this.invertedIndex = new PostingTable();
	}
}
