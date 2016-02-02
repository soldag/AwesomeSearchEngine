package indexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import documents.PatentContentDocument;
import documents.PatentDocument;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;
import indexing.citations.CitationIndexConstructor;
import indexing.citations.CitationIndexSeekList;
import indexing.contents.ContentsIndexConstuctor;
import indexing.contents.ContentsIndexMerger;
import indexing.contents.ContentsIndexSeekList;
import indexing.documentmap.DocumentMapConstructor;
import indexing.documentmap.DocumentMapSeekList;
import indexing.invertedindex.InvertedIndexConstructor;
import indexing.invertedindex.InvertedIndexMerger;
import indexing.invertedindex.InvertedIndexSeekList;
import parsing.PatentDocumentParser;
import postings.ContentType;
import querying.ranking.PageRankCalculator;
import textprocessing.TextPreprocessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class DocumentIndexer {
	
	/**
	 * Determines the memory limit. If this limit is exceeded, memory index has to be written to file.
	 */
	private static final int MEMORY_LIMIT = 2000000000;
	
	/**
	 * Contains the prefix for temporary index files.
	 */
	private static final String TEMP_INVERTED_INDEX_PREFIX = "awse_index_%d";
	private static final String TEMP_CONTENTS_INDEX_PREFIX = "awse_contents_%d";
	
	/**
	 * Contains the content types, that should be stored in the contents index.
	 */
	private static final ContentType[] CONTENT_TYPES_TO_STORE = new ContentType[] {ContentType.Title, ContentType.Abstract};
	
	/**
	 * Contains necessary services.
	 */
	private TextPreprocessor textPreprocessor;
	private PageRankCalculator pageRankCalculator;
		
	/**
	 * Contain the constructors for the several indexes.
	 */
	private InvertedIndexConstructor invertedIndexConstructor;
	private DocumentMapConstructor documentMapConstructor;
	private ContentsIndexConstuctor contentsIndexConstructor;
	private CitationIndexConstructor citationIndexConstructor;
	
	/**
	 *  Contain index files that should be constructed.
	 */
	private File indexFile;
	private File indexSeekListFile;
	private File documentMapFile;
	private File documentMapSeekListFile;
	private File contentsIndexFile;
	private File contentsIndexSeekListFile;
	private File citationIndexFile;
	private File citationIndexSeekListFile;
	
	/**
	 * Determines, whether the index should be compressed or not.
	 */
	private boolean compress;
	
	
	/**
	 * Contains for each patent the ids of documents, that it cites.
	 */
	private Map<PatentDocument, TIntList> linkedDocuments = new HashMap<PatentDocument, TIntList>();
	
	/**
	 * Contains all created temporary index files.
	 */
	private List<File> tempInvertedIndexFiles = new ArrayList<File>();
	private List<File> tempContentsIndexFiles = new ArrayList<File>();
	
	/**
	 * Creates a new DocumentIndexer instance.
	 * @param textProcessor
	 * @param pageRankCalculator
	 * @param indexFile
	 * @param seekListFile
	 * @param documentMapFile
	 * @param documentMapSeekListFile
	 * @param citationIndexFile
	 * @param citationIndexSeekListFile
	 * @param compress
	 */
	public DocumentIndexer(TextPreprocessor textProcessor, PageRankCalculator pageRankCalculator, File indexFile, File seekListFile, 
			File documentMapFile, File documentMapSeekListFile, File contentsIndexFile, File contentsIndexSeekListFile, 
			File citationIndexFile, File citationIndexSeekListFile, boolean compress) {
		this.textPreprocessor = textProcessor;
		this.pageRankCalculator = pageRankCalculator;
		
		this.indexFile = indexFile;
		this.indexSeekListFile = seekListFile;
		this.documentMapSeekListFile = documentMapSeekListFile;
		this.documentMapFile = documentMapFile;
		this.contentsIndexFile = contentsIndexFile;
		this.contentsIndexSeekListFile = contentsIndexSeekListFile;
		this.citationIndexFile = citationIndexFile;
		this.citationIndexSeekListFile = citationIndexSeekListFile;
		this.compress = compress;
		
		this.invertedIndexConstructor = new InvertedIndexConstructor(this.compress, new InvertedIndexSeekList());
		this.documentMapConstructor = new DocumentMapConstructor(this.compress, new DocumentMapSeekList());
		this.contentsIndexConstructor = new ContentsIndexConstuctor(compress, new ContentsIndexSeekList());
		this.citationIndexConstructor = new CitationIndexConstructor(compress, new CitationIndexSeekList());
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
		
		// Calculate page ranks
		List<PatentDocument> documents = this.pageRankCalculator.calculate(this.linkedDocuments);
		
		// Construct document map and inverse citation index
		this.constructDocumentMap(documents);
		this.constructCitationIndex();
		
		// Write constructed inverted index to file
		this.writeFinalInvertedIndex();
		
		// Write constructed contents index to file
		this.writeFinalContentsIndex();
		
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
			
			// Adds contents to the index
			this.contentsIndexConstructor.put(document, CONTENT_TYPES_TO_STORE);
			
			// Add document linked document map
			this.linkedDocuments.put(document.withoutContent(), new TIntArrayList(document.getLinkedDocumentIds()));
			
			// If memory consumption is too high, write inverted and contents index to new temporary file and clean memory.
			if(this.getFreeMemory() < MEMORY_LIMIT) {
				System.out.println("Write temp files...");
				this.writeTemporaryContentsIndex();
				this.writeTemporaryInvertedIndex();
				
				// Run garbage collector
				System.gc();
				System.runFinalization();
			}
		}
	}
	
	/**
	 * Gets the amount of free memory in bytes.
	 * @return
	 */
	private long getFreeMemory() {
		return Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
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
						this.invertedIndexConstructor.add(document.getId(), token, contentType, position);
					}
				}
			}
			
			tokenCounts.put(contentType, tokens.size());
		}
		
		document.setTokensCount(tokenCounts);
	}
	
	
	/**
	 * Creates the document map from document list and stored it to file.
	 * @throws IOException
	 */
	private void constructDocumentMap(List<PatentDocument> documents) throws IOException {
		for(PatentDocument document: documents) {
			this.documentMapConstructor.add(document);
		}
		this.documentMapConstructor.writeToFile(this.documentMapFile, this.documentMapSeekListFile);
	}
	
	/**
	 * Creates the inverse citation index from linked document map and stored it to file.
	 * @throws IOException
	 */
	private void constructCitationIndex() throws IOException {
		for(Map.Entry<PatentDocument, TIntList> entry: this.linkedDocuments.entrySet()) {
			int documentId = entry.getKey().getId();
			TIntList linkedDocumentIds = entry.getValue();
			linkedDocumentIds.forEach(new TIntProcedure() {					
				@Override
				public boolean execute(int linkedDocumentId) {
					citationIndexConstructor.add(linkedDocumentId, documentId);
					return true;
				}
			});
		}
		this.citationIndexConstructor.writeToFile(this.citationIndexFile, this.citationIndexSeekListFile);
	}
	
	
	/**
	 * Write inverted index from memory to temporary index files.
	 * @throws IOException
	 */	
	private void writeTemporaryInvertedIndex() throws IOException {
		File invertedIndexFile = File.createTempFile(TEMP_INVERTED_INDEX_PREFIX, "");
		this.tempInvertedIndexFiles.add(invertedIndexFile);		
		this.invertedIndexConstructor.writeToFile(invertedIndexFile);
		this.invertedIndexConstructor.clear();
	}
	
	/**
	 * Write contents index from memory to temporary index files.
	 * @throws IOException
	 */	
	private void writeTemporaryContentsIndex() throws IOException {
		File contentsIndexFile = File.createTempFile(TEMP_CONTENTS_INDEX_PREFIX, "");
		this.tempContentsIndexFiles.add(contentsIndexFile);
		this.contentsIndexConstructor.writeToFile(contentsIndexFile);
		this.contentsIndexConstructor.clear();
	}
	
	/**
	 * Write inverted index from memory to final index file. 
	 * @throws IOException
	 */
	private void writeFinalInvertedIndex() throws IOException {
		if(this.tempInvertedIndexFiles.isEmpty()) {
			this.invertedIndexConstructor.writeToFile(this.indexFile, this.indexSeekListFile);
			this.invertedIndexConstructor.clear();
		}
		else {
			if(this.invertedIndexConstructor.size() > 0) {
				this.writeTemporaryInvertedIndex();
			}
			
			System.out.println("Merge inverted index files...");
			InvertedIndexMerger indexMerger = new InvertedIndexMerger(this.compress);
			indexMerger.merge(this.indexFile, this.tempInvertedIndexFiles, this.indexSeekListFile);
		}
	}
	
	/**
	 * Write contents index from memory to final index file. 
	 * @throws IOException
	 */
	private void writeFinalContentsIndex() throws IOException {
		if(this.tempContentsIndexFiles.isEmpty()) {
			this.contentsIndexConstructor.writeToFile(this.contentsIndexFile, this.contentsIndexSeekListFile);
			this.contentsIndexConstructor.clear();
		}
		else {
			if(this.contentsIndexConstructor.size() > 0) {
				this.writeTemporaryContentsIndex();
			}
			
			System.out.println("Merge contents index files...");
			ContentsIndexMerger indexMerger = new ContentsIndexMerger(this.compress);
			indexMerger.merge(this.contentsIndexFile, this.tempContentsIndexFiles, this.indexSeekListFile);
		}
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

		// Contents index
		for(File indexFile: this.tempContentsIndexFiles) {
			if(indexFile.exists()) {
				indexFile.delete();
			}
		}
		this.tempContentsIndexFiles.clear();
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
		
		if(this.citationIndexSeekListFile.exists()) {
			this.citationIndexSeekListFile.delete();
		}
		if(this.citationIndexFile.exists()) {
			this.citationIndexFile.delete();
		}
	}
}
