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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import evaluation.NdcgCalculator;
import indexing.DocumentIndexer;
import parsing.PatentContentLookup;
import querying.DocumentRanker;
import querying.QueryProcessor;
import querying.queries.QueryParser;
import querying.results.QueryResult;
import querying.spellingcorrection.DamerauLevenshteinCalculator;
import textprocessing.TextPreprocessor;
import visualization.ResultFormatter;
import visualization.SnippetGenerator;

public class AwesomeSearchEngine extends SearchEngine {
	
	/**
	 * Contains the file name pattern for documents supported by this search engine. 
	 */
	private static final String DOCUMENT_FILE_PATTERN = "ipg\\d+.xml";

	/**
	 * Contain instances of necessary services.
	 */
	private TextPreprocessor textPreprocessor;
	private DocumentIndexer documentIndexer;
	private QueryParser queryParser;
	private QueryProcessor queryProcessor;
	private PatentContentLookup patentContentLookup;
	private SnippetGenerator snippetGenerator;
	private ResultFormatter resultFormatter;
	private NdcgCalculator ncdgCalculator;
    
	/**
	 * Contains the path of the directory containing the patent data.
	 */
	private Path documentDirectory;
	
	/**
	 *  Contain the necessary index files.
	 */
	private final Path teamDirectoryPath = Paths.get(teamDirectory);
	private final File indexFile = this.teamDirectoryPath.resolve("index.bin").toFile();
	private final File indexSeekListFile = this.teamDirectoryPath.resolve("index_seek_list.bin").toFile();
	private final File documentMapFile = this.teamDirectoryPath.resolve("document_map.bin").toFile();
	private final File documentMapSeekListFile = this.teamDirectoryPath.resolve("document_map_seek_list.bin").toFile();
	private final File stopWordsFile = this.teamDirectoryPath.resolve("stop_words.txt").toFile();
	
	/**
	 * Creates a new AwesomeSearchEngine instance.
	 */
    public AwesomeSearchEngine() { 
        // This should stay as is! Don't add anything here!
        super();
    }
    
    /**
     * Returns the current text processor. 
     * @return
     */
    private TextPreprocessor getTextPreprocessor() {
    	if(this.textPreprocessor == null) {
    		this.textPreprocessor = new TextPreprocessor();
    		try {
				this.textPreprocessor.loadStopWords(stopWordsFile);
			} catch (IOException e) {
	        	System.err.println("Stop words could not be loaded!");
			}
    	}
    	
    	return this.textPreprocessor;
    }
    
    /**
     * Returns the current document indexer.
     * @param compress
     * @return
     */
    private DocumentIndexer getDocumentIndexer(boolean compress) {
    	if(this.documentIndexer == null) {
    		this.documentIndexer = new DocumentIndexer(
    						this.getTextPreprocessor(), 
    						this.indexFile, 
    						this.indexSeekListFile, 
    						this.documentMapFile, 
    						this.documentMapSeekListFile, 
    						compress);
    	}
    	
    	return this.documentIndexer;
    }
    
    private QueryParser getQueryParser() {
    	if(this.queryParser == null) {
    		this.queryParser = new QueryParser(this.getTextPreprocessor());
    	}
    	
    	return this.queryParser;
    }
    
    /**
     * Returns the current query processor.
     * @param isCompressed
     * @return
     */
    private QueryProcessor getQueryProcessor(boolean isCompressed) {
    	if(this.queryProcessor == null || this.queryProcessor.isCompressed() != isCompressed) {
    		try {
				this.queryProcessor = new QueryProcessor(
					this.getQueryParser(),
					this.getTextPreprocessor(), 
					new DamerauLevenshteinCalculator(1, 1, 1, 1),
					new DocumentRanker(),
					this.getSnippetGenerator(),
					this.documentMapFile, 
					this.documentMapSeekListFile, 
					this.indexFile, 
					this.indexSeekListFile,
					isCompressed);
			} catch (FileNotFoundException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}
    	}
    	
    	return this.queryProcessor;
    }
    
    /**
     * Returns the current content lookup.
     * @return
     */
    private PatentContentLookup getPatentContentLookup() {
    	if(this.patentContentLookup == null) {
    		this.patentContentLookup = new PatentContentLookup(this.documentDirectory);
    	}
    	
    	return this.patentContentLookup;
    }
    
    /**
     * Returns the current snippet generator
     * @return
     */
    private SnippetGenerator getSnippetGenerator() {
    	if(this.snippetGenerator == null) {
    		this.snippetGenerator = new SnippetGenerator(
    				this.getTextPreprocessor(), 
    				this.getPatentContentLookup());
    	}
    	
    	return this.snippetGenerator;
    }
    
    /**
     * Returns the current result formatter.
     * @return
     */
    private ResultFormatter getResultFormatter() {
    	if(this.resultFormatter == null) {
    		this.resultFormatter = new ResultFormatter(
    				this.getPatentContentLookup(), 
    				this.getSnippetGenerator());
    	}
    	
    	return this.resultFormatter;
    }
    
    /**
     * Returns the current ncdg calculator.
     * @return
     */
    private NdcgCalculator getNcdgCalculator() {
    	if(ncdgCalculator == null) {
    		this.ncdgCalculator = new NdcgCalculator();
    	}
    	
		return ncdgCalculator;
	}
    

    @Override
    public void index(String directory) {
    	this.index(directory, false);
    }

    @Override
    public boolean loadIndex(String directory) {
    	return this.loadIndex(false);
    }
    
    @Override
    public void compressIndex(String directory) {
    	this.index(directory, true);
    }

    @Override
    boolean loadCompressedIndex(String directory) {
    	return this.loadIndex(true);
    }
    
    @Override
    public ArrayList<String> search(String query, int topK, int prf) {
    	if(queryProcessor != null && this.queryProcessor.isReady()) {    	
	    	try {
	    		QueryResult result = this.queryProcessor.search(query, topK);
	    		
	    		return this.getResultFormatter().format(result);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	else {
			System.err.println("Index has to be loaded first. ");
		}
    	
		return new ArrayList<String>();
    }


	@Override
	public Double computeNdcg(ArrayList<String> goldRanking, ArrayList<String> ranking, int p) {
		return this.getNcdgCalculator().calculate(goldRanking, ranking, p);
	}
	
    
    /**
     * Indexes all documents of a given directory. Argument compress determines, whether the index should be compressed.
     * @param directory
     * @param compress
     */
    private void index(String directory, boolean compress) {
    	// Check, if given path exists and is a directory
    	this.documentDirectory = Paths.get(directory);
    	if(!this.documentDirectory.toFile().isDirectory()) {
			System.err.println("Specified document directory is not a directory!");
			System.exit(1);
    	}
    	
    	try {
        	// Get xml files inside given directory
    		String[] documentFiles = Files.walk(this.documentDirectory)
    				.map(path -> path.toString())
    				.filter(path -> FilenameUtils.getName(path).matches(DOCUMENT_FILE_PATTERN))
    				.toArray(String[]::new);
    		
    		// Build index
			this.getDocumentIndexer(compress).indexDocumentFiles(documentFiles);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
    }
    
    /**
     * Loads the index from disk to memory.
     * @param compress
     * @return
     */
    private boolean loadIndex(boolean compress) {
    	try {
			this.getQueryProcessor(compress).load();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
        
        return true;
    }
}
