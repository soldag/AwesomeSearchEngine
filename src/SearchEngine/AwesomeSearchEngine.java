package SearchEngine;

import java.io.File;

/**
 *
 * @author: Henriette Dinger & Soeren Oldag
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 *
 * Implementation decisions:
 * - We used a stop word list from http://xpo6.com/list-of-english-stop-words/.
 * - We used the Apache Lucene library for stemming and tokenizing.
 * - The index is first written in several smaller files. The limit for one file can be set with a constant. After that the files were merged with a variant of the two phase multi-way merge sort. An index entry consists of the word and a list of tuples of the form [doc-id, offset-position].
 * - There is a second kind of index created which maps the document-ID of patents to the file the patents are stored in.  
 * - The seek list contains the index word and the offset position of the word in the index file. Thereby not all words are stored in the seek list, but every X entries where X is a changeable constant.
 */

import java.io.FileNotFoundException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

public class AwesomeSearchEngine extends SearchEngine { 
	
	// Constants for index file names
	private static final String INDEX_FILE_NAME = "index";
	private static final String SEEK_LIST_FILE_NAME = "seek_list";
	private static final String DOCUMENT_MAP_FILE_NAME = "document_map";
	
	// Contain the paths to the index files
	private File indexFile;
	private File seekListFile;
	private File documentIndexFile;

	// Contain instances of necessary services
	private AwesomeTextProcessor textProcessor;
	private AwesomeIndexer indexer;
	private AwesomeQueryProcessor queryProcessor;
	
	// Determines, whether web search was already initialized
	// (necessary services are instantiated and initialized)
	private Boolean isInitialized = false;
    
    public AwesomeSearchEngine() { 
        // This should stay as is! Don't add anything here!
        super();
    }
    
    public void initalize() {
    	// Setup paths
    	Path indexDirectory = Paths.get(teamDirectory);
    	this.indexFile = indexDirectory.resolve(INDEX_FILE_NAME).toFile();
    	this.seekListFile = indexDirectory.resolve(SEEK_LIST_FILE_NAME).toFile();
    	this.documentIndexFile = indexDirectory.resolve(DOCUMENT_MAP_FILE_NAME).toFile();
    	
    	// Instantiate necessary services
        this.textProcessor = new AwesomeTextProcessor();
        this.indexer = new AwesomeIndexer(this.textProcessor, indexDirectory, this.indexFile, this.seekListFile, this.documentIndexFile);
        this.queryProcessor = new AwesomeQueryProcessor(this.textProcessor, this.indexFile, this.seekListFile, this.documentIndexFile);
        
        // Load stop words for text processing
        try {
        	this.textProcessor.loadStopWords();
        }
        catch(IOException e) {
        	System.err.println("Stop words could not be loaded!");
        }
        
        this.isInitialized = true;
    }

    @Override
    void index(String directory) {
    	if(!this.isInitialized) {
    		this.initalize();
    	}
    	
    	try {
			this.indexer.parseDocument(directory + "/testData.xml");
		} catch (IOException|XMLStreamException e) {
			e.printStackTrace();
		}
    }

    @Override
    boolean loadIndex(String directory) {
    	if(!this.isInitialized) {
    		this.initalize();
    	}
    	
    	try {
			queryProcessor.loadIndex();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
        
        return true;
    }
    
    @Override
    void compressIndex(String directory) {
    }

    @Override
    boolean loadCompressedIndex(String directory) {
        return false;
    }
    
    @Override
    ArrayList<String> search(String query, int topK, int prf) {
    	if(!this.isInitialized) {
    		this.initalize();
    	}
    	
    	try {
			return this.queryProcessor.search(query);
		} catch (IOException|XMLStreamException e) {
			e.printStackTrace();
		}
        
        return null;
    }
}
