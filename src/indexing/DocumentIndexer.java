package indexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import documents.PatentContentDocument;
import indexing.documentmap.DocumentMapConstructor;
import indexing.documentmap.DocumentMapMerger;
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
	 * Determines the memory limit. If this limit is exceeded, memory index has to be written to file.
	 */
	private static final int MEMORY_LIMIT = 1000000000;
	
	/**
	 * Contains the prefix for temporary index files.
	 */
	private static final String TEMP_INVERTED_INDEX_PREFIX = "awse_index_%d";
	private static final String TEMP_DOCUMENT_MAP_PREFIX = "awse_document_map_%d";
	
	/**
	 * Contains text preprocessor instance.
	 */
	private TextPreprocessor textPreprocessor;
		
	/**
	 * Contain the constructors for the inverted index and document map.
	 */
	private InvertedIndexConstructor indexConstructor;
	private DocumentMapConstructor documentMapConstructor;
	
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
	private List<File> tempInvertedIndexFiles = new ArrayList<File>();
	private List<File> tempDocumentMapFiles = new ArrayList<File>();
	
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
		this.documentMapConstructor = new DocumentMapConstructor(this.compress, new DocumentMapSeekList());
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
		for(int i = 0; i < documentPaths.length; i++) {
			System.out.println(String.format("Index document %d/%d...", i + 1, documentPaths.length));
			
			String documentPath = documentPaths[i];
			try {
				this.indexSingleDocumentFile(documentPath);
			} catch (XMLStreamException e) {
				String fileName = Paths.get(documentPath).getFileName().toString();
				System.out.println(String.format("File '%s' could not be parsed and was skipped.", fileName));
			}
		}
		
		// Write constructed indexes to files
		this.writeFinalIndexFiles();
		
		// Delete temporary files
		this.clearTemporaryIndexes();
	}
	
	/**
	 * Indexes a single document. 
	 * @param documentFilePath
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	private void indexSingleDocumentFile(String documentFilePath) throws XMLStreamException, IOException {
		PatentDocumentParser patentParser = new PatentDocumentParser(documentFilePath);
		for(PatentContentDocument document: patentParser) {
			// Add all tokens from document to memory index
			this.addTokens(document);
			
			// Add document to document map
			this.documentMapConstructor.add(document.withoutContent());
			
			// If size of the memory indexes is too high, write them to new temporary file and clean memory.
			if(this.indexConstructor.entriesCount() > 1000000) {
				//System.out.println("Write memory index to temp file, because memory limit has exceeded.");
				this.writeTemporaryInvertedIndex();
				
				// Run garbage collector
				System.gc();
				System.runFinalization();
			}
			if(this.documentMapConstructor.entriesCount() > 1000000) {
				this.writeTemporaryDocumentMap();
				
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
				String token = tokens.get(position);
				if(!this.textPreprocessor.isStopWord(token)) {
					// Stem token
					token = this.textPreprocessor.stem(token);
					
					// Add posting to memory index
					if(!token.isEmpty()) {
						this.indexConstructor.add(document.getId(), token, contentType, position);
					}
				}
			}
			
			tokenCounts.put(contentType, tokens.size());
		}
		
		document.setTokensCount(tokenCounts);
	}	
	
	/**
	 * Returns number of used memory bytes.
	 * @return long
	 */
	private long getUsedMemory() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}
	
	
	/**
	 * Write inverted index from memory to temporary index files.
	 * @throws IOException
	 */	
	private void writeTemporaryInvertedIndex() throws IOException {
		File invertedIndexFile = File.createTempFile(TEMP_INVERTED_INDEX_PREFIX, "");
		this.tempInvertedIndexFiles.add(invertedIndexFile);		
		this.indexConstructor.writeToFile(invertedIndexFile);
		this.indexConstructor.clear();
	}
	
	/**
	 * Write document map from memory to temporary index files.
	 * @throws IOException
	 */
	private void writeTemporaryDocumentMap() throws IOException {
		File documentMapFile = File.createTempFile(TEMP_DOCUMENT_MAP_PREFIX, "");
		this.tempDocumentMapFiles.add(documentMapFile);		
		this.documentMapConstructor.writeToFile(documentMapFile);
		this.documentMapConstructor.clear();
	}
	
	/**
	 * Write constructed indexes to final files.
	 * Merging of temporary files may be necessary.
	 * @throws IOException
	 */
	private void writeFinalIndexFiles() throws IOException {		
		// Write remaining document map entries to file and merge temporary files, if necessary
		if(this.tempDocumentMapFiles.isEmpty()) {
			this.writeFinalDocumentMap();
		}
		else {
			if(this.documentMapConstructor.size() > 0) {
				this.writeTemporaryDocumentMap();
			}
			
			System.out.println("Merge document map files...");
			DocumentMapMerger indexMerger = new DocumentMapMerger(this.compress);
			indexMerger.merge(this.documentMapFile, this.tempDocumentMapFiles, this.documentMapSeekListFile);
		}
		
		// Write remaining inverted index entries to file and merge temporary files, if necessary
		if(this.tempInvertedIndexFiles.isEmpty()) {
			this.writeFinalInvertedIndex();
		}
		else {
			if(this.indexConstructor.size() > 0) {
				this.writeTemporaryInvertedIndex();
			}
			
			System.out.println("Merge inverted index files...");
			InvertedIndexMerger indexMerger = new InvertedIndexMerger(this.compress);
			indexMerger.merge(this.indexFile, this.tempInvertedIndexFiles, this.indexSeekListFile);
		}
	}
	
	/**
	 * Write inverted index from memory to final index file. 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeFinalInvertedIndex() throws IOException {
		this.indexConstructor.writeToFile(this.indexFile, this.indexSeekListFile);
		this.indexConstructor.clear();
	}
	
	/**
	 * Write document map from memory to final index file. 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeFinalDocumentMap() throws IOException {
		this.documentMapConstructor.writeToFile(this.documentMapFile, this.documentMapSeekListFile);
		this.documentMapConstructor.clear();
	}
	
	/**
	 * Deletes all temporary index files.
	 * @throws IOException
	 */
	public void clearTemporaryIndexes() throws IOException {
		// Inverted index
		for(File indexFile: this.tempInvertedIndexFiles) {
			if(indexFile.exists()) {
				indexFile.delete();
			}
		}
		this.tempInvertedIndexFiles.clear();
		
		// Document map
		for(File indexFile: this.tempDocumentMapFiles) {
			if(indexFile.exists()) {
				indexFile.delete();
			}
		}
		this.tempDocumentMapFiles.clear();
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
