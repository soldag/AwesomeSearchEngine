package indexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import documents.PatentContentDocument;
import indexing.documentmap.DocumentMapConstructor;
import indexing.documentmap.DocumentMapSeekList;
import indexing.invertedindex.InvertedIndexConstructor;
import indexing.invertedindex.InvertedIndexMerger;
import indexing.invertedindex.InvertedIndexSeekList;
import parsing.PatentDocumentParser;
import postings.ContentType;
import textprocessing.TextPreprocessor;

import java.io.File;
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
	public void indexDocumentFiles(String[] documentPaths) throws IOException {
		// Delete existing indexes
		this.deleteIndexFiles();

		// Parse and index documents
		try(DocumentMapConstructor documentMapConstructor = new DocumentMapConstructor(this.documentMapFile, new DocumentMapSeekList())) {
			for(String documentPath: documentPaths) {
				try {
					this.indexSingleDocumentFile(documentPath, documentMapConstructor);
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
	 * @param documentFilePath
	 * @param indexConstructor
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	private void indexSingleDocumentFile(String documentFilePath, DocumentMapConstructor documentMapConstructor) throws XMLStreamException, IOException {
		PatentDocumentParser patentParser = new PatentDocumentParser(documentFilePath);
		for(PatentContentDocument document: patentParser) {
			// Add all tokens from document to memory index
			this.addTokens(document);
			
			// Add document to document map
			documentMapConstructor.add(document);
			
			// If size of the memory index is too high, write memory index to new temporary file and clear memory
			if(this.getFreeMemory() < FREE_MEMORY_LIMIT) {
				this.writeToTempFile();
				
				// Run garbage collector
				System.gc();
				System.runFinalization();
			}
		}
	}
	
	/**
	 * Adds all tokens from document to memory index and return number of inserted tokens.
	 * @param document
	 * @return
	 * @throws IOException
	 */
	private void addTokens(PatentContentDocument document) throws IOException{
		Map<ContentType, Integer> tokenCounts = new HashMap<ContentType, Integer>();
		for(ContentType contentType: ContentType.values()) {
			// Tokenize, stem and remove stop-words from content, and add single tokens to index
			List<String> tokens = this.textPreprocessor.tokenize(document.getContent(contentType));
			for(int position = 0; position < tokens.size(); position++) {
				String token = this.textPreprocessor.stem(tokens.get(position));
				if(!this.textPreprocessor.isStopWord(token)) {
					// Stem token
					token = this.textPreprocessor.stem(token);
					
					// Add posting to memory index
					this.indexConstructor.add(document.getId(), token, contentType, position);
				}
			}
			
			tokenCounts.put(contentType, tokens.size());
		}
		
		document.setTokensCount(tokenCounts);
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