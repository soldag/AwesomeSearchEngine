package indexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import SearchEngine.PatentAbstractDocument;
import SearchEngine.Posting;
import parsing.PatentDocumentParser;
import textprocessing.TextPreprocessor;
import utilities.MapValueComparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

public class DocumentIndexer {
	
	/**
	 * Determines the limit of free bytes in memory. If this limit is exceeded, memory index has to be written to file.
	 */
	private static final int FREE_MEMORY_LIMIT = 512000000;
	
	/**
	 * Contains the prefix for temporary index files.
	 */
	private static final String TEMP_INDEX_PREFIX = "awse_index_%d";

	/**
	 * Contains the number of the top most frequent tokens that should be stored in the document map.
	 */
	private static final int MOST_FREQUENT_TOKENS_LIMIT = 5;
	
	/**
	 * Contains text preprocessor instance.
	 */
	private TextPreprocessor textPreprocessor;
	
	/**
	 * Contains the service for constructing the index.
	 */
	private InvertedIndexConstructor indexConstructor;
	
	/**
	 *  Contain index files that should be constructed.
	 */
	private File indexFile;
	private File indexSeekListFile;
	private File documentMapFile;
	private File documentMapSeekListFile;
	
	/**
	 * Determines, whether the index should be compressed or not.
	 */
	private boolean compress;
	
	/**
	 * Contains all created temporary index files.
	 */
	private List<File> temporaryIndexFiles = new ArrayList<File>();
	
	/**
	 * Contains the number of occurrences per token of a single document.
	 */
	private Map<String, Integer> tokenOccurrencesCount = new HashMap<String, Integer>();
	
	/**
	 * Creates a new DocumentIndexer instance.
	 * @param textProcessor
	 * @param indexFile
	 * @param seekListFile
	 * @param documentMapFile
	 * @param documentMapSeekListFile
	 * @param compress
	 */
	public DocumentIndexer(TextPreprocessor textProcessor, File indexFile, File seekListFile, File documentMapFile, File documentMapSeekListFile, boolean compress) {
		this.textPreprocessor = textProcessor;
		
		this.indexFile = indexFile;
		this.indexSeekListFile = seekListFile;
		this.documentMapSeekListFile = documentMapSeekListFile;
		this.documentMapFile = documentMapFile;
		this.compress = compress;
		
		this.indexConstructor = new InvertedIndexConstructor(this.compress, new InvertedIndexSeekList());
	}
	
	
	/**
	 * Indexes the given XML documents. Old index files are overwritten.
	 * @param documentPaths
	 * @throws IOException
	 */
	public void indexDocuments(String[] documentPaths) throws IOException {
		// Delete existing indexes
		this.deleteIndexFiles();

		// Parse and index documents
		try(DocumentMapConstructor documentMapConstructor = new DocumentMapConstructor(this.documentMapFile, new DocumentMapSeekList())) {
			for(String documentPath: documentPaths) {
				try {
					this.indexSingleDocument(documentPath, documentMapConstructor);
				} catch (XMLStreamException e) {
					String fileName = Paths.get(documentPath).getFileName().toString();
					System.out.println(String.format("File '%s' could not be parsed and was skipped.", fileName));
				}
			}
			
			documentMapConstructor.writeSeekList(this.documentMapSeekListFile);
		}
		
		// Write remaining index entries to file and merge temporary files, if necessary
		if(this.temporaryIndexFiles.size() == 0) {
			this.writeToFinalFile();
		}
		else {
			if(indexConstructor.size() > 0) {
				this.writeToTempFile();
			}
			InvertedIndexMerger indexMerger = new InvertedIndexMerger(this.compress, new InvertedIndexSeekList());
			indexMerger.merge(this.indexFile, this.temporaryIndexFiles, this.indexSeekListFile);
		}
		
		// Delete temporary files
		this.clearTemporaryIndexes();
	}
	
	/**
	 * Indexes a single document. 
	 * @param documentPath
	 * @param indexConstructor
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	private void indexSingleDocument(String documentPath, DocumentMapConstructor documentMapConstructor) throws XMLStreamException, IOException {
		try(PatentDocumentParser patentParser = new PatentDocumentParser(new FileInputStream(documentPath))) {
			for(PatentAbstractDocument document: patentParser) {				
				// Tokenize, stem and remove stop-words from abstract, and add single tokens to index
				List<String> tokens = this.textPreprocessor.removeStopWords(this.textPreprocessor.tokenize(document.getAbstractText()));
				for(int i = 0; i < tokens.size(); i++) {
					this.addToken(document, tokens.get(i), i);
				}
				
				// Count the most frequent terms
				List<String> mostFrequentTokens = tokenOccurrencesCount.entrySet().stream()
													.sorted(Collections.reverseOrder(new MapValueComparator<String, Integer>()))
													.limit(MOST_FREQUENT_TOKENS_LIMIT)
													.map(x -> x.getKey())
													.collect(Collectors.toList());
				tokenOccurrencesCount.clear();
				
				// Add to document map
	    		document.setTokensCount(tokens.size());
	    		document.setMostFrequentTokens(mostFrequentTokens);
				documentMapConstructor.add(document);
			}
		}
	}
	
	/**
	 * Adds given token to memory index. 
	 * @param document
	 * @param token
	 * @param position
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void addToken(PatentAbstractDocument document, String token, int position) throws FileNotFoundException, IOException {
		// Stem token
		token = this.textPreprocessor.stem(token);
		
		// Add posting to memory index
		Posting posting = new Posting(document.getId(), position);
		indexConstructor.add(token, posting);
		
		// If size of the memory index is too high, write memory index to new temporary file and clear memory
		if(this.getFreeMemory() < FREE_MEMORY_LIMIT) {
			this.writeToTempFile();
			
			// Run garbage collector
			System.gc();
			System.runFinalization();
		}
		
		// Increase number of occurrences in current document
		if(tokenOccurrencesCount.containsKey(token)){
			tokenOccurrencesCount.put(token, tokenOccurrencesCount.get(token) + 1);
		}
		else {
			tokenOccurrencesCount.put(token, 1);
		}		
	}	
	
	/**
	 * Returns number of free memory bytes.
	 * @return long
	 */
	private long getFreeMemory() {
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		return Runtime.getRuntime().maxMemory() - usedMemory;
	}
	
	
	/**
	 * Write memory index to temporary index file.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeToTempFile() throws FileNotFoundException, IOException {
		File tempIndexFile = File.createTempFile(TEMP_INDEX_PREFIX, "");
		this.temporaryIndexFiles.add(tempIndexFile);
		
		this.indexConstructor.writeToFile(tempIndexFile);
		this.indexConstructor.clear();
	}
	
	/**
	 * Write memory index to final index file. 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeToFinalFile() throws FileNotFoundException, IOException {
		this.indexConstructor.writeToFile(this.indexFile, this.indexSeekListFile);
		this.indexConstructor.clear();
	}
	
	/**
	 * Deletes all temporary index files.
	 * @throws IOException
	 */
	public void clearTemporaryIndexes() throws IOException {
		for(File indexFile: this.temporaryIndexFiles) {
			if(indexFile.exists()) {
				indexFile.delete();
			}
		}
		this.temporaryIndexFiles.clear();
	}
	
	/**
	 * Deletes all existing index files.
	 */
	public void deleteIndexFiles() {
		if(this.indexFile.exists()) {
			this.indexFile.delete();
		}
		if(this.indexSeekListFile.exists()) {
			this.indexSeekListFile.delete();
		}
		if(this.documentMapSeekListFile.exists()) {
			this.documentMapSeekListFile.delete();
		}
		if(this.documentMapFile.exists()) {
			this.documentMapFile.delete();
		}
	}
}
