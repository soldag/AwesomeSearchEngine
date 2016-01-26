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
import indexing.citations.CitationIndexReader;
import indexing.documentmap.DocumentMapReader;
import indexing.invertedindex.InvertedIndexReader;
import parsing.PatentContentLookup;
import querying.QueryProcessor;
import querying.queries.QueryParser;
import querying.ranking.DocumentRanker;
import querying.results.RankedQueryResult;
import querying.spellingcorrection.DamerauLevenshteinCalculator;
import querying.spellingcorrection.SpellingCorrector;
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
	private DamerauLevenshteinCalculator levenshteinCalculator;
	private SpellingCorrector spellingCorrector;
	private PatentContentLookup patentContentLookup;
	private SnippetGenerator snippetGenerator;
	private ResultFormatter resultFormatter;
	private NdcgCalculator ncdgCalculator;
	
	/**
	 * Contain the index reader services
	 */
	private InvertedIndexReader invertedIndexReader;
	private DocumentMapReader documentMapReader;
	private CitationIndexReader citationIndexReader;
	
	/**
	 * Determines, whether the index has already been read into memory.
	 */
	private boolean isLoaded = false;
	
	/**
	 * Contain necessary directory paths.
	 */
	private final Path teamDirectoryPath = Paths.get(teamDirectory);
	private final Path dataDirectoryPath = Paths.get(dataDirectory);
	
	/**
	 *  Contain the necessary index files.
	 */
	private final File indexFile = this.teamDirectoryPath.resolve("inverted_index.bin").toFile();
	private final File indexSeekListFile = this.teamDirectoryPath.resolve("inverted_index_seek_list.bin").toFile();
	private final File documentMapFile = this.teamDirectoryPath.resolve("document_map.bin").toFile();
	private final File documentMapSeekListFile = this.teamDirectoryPath.resolve("document_map_seek_list.bin").toFile();
	private final File citationIndexFile = this.teamDirectoryPath.resolve("citation_index.bin").toFile();
	private final File citationIndexSeekListFile = this.teamDirectoryPath.resolve("citation_index_seek_list.bin").toFile();
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
    						this.citationIndexFile,
    						this.citationIndexSeekListFile,
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
     * @return
     */
    private QueryProcessor getQueryProcessor() {
    	if(this.queryProcessor == null) {
    		try {
				this.queryProcessor = new QueryProcessor(
					this.invertedIndexReader,
					this.citationIndexReader,
					this.getQueryParser(),
					this.getTextPreprocessor(),
					this.getSpellingCorrector(),
					new DocumentRanker(this.documentMapReader),
					this.getSnippetGenerator());
			} catch (FileNotFoundException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}
    	}
    	
    	return this.queryProcessor;
    }
    
    /**
     * Returns the current levenshtein calculator.
     * @return
     */
    private DamerauLevenshteinCalculator getLevenshteinCalculator() {
    	if(this.levenshteinCalculator == null) {
    		this.levenshteinCalculator = new DamerauLevenshteinCalculator(1, 1, 1, 1);
    	}
    	
    	return this.levenshteinCalculator;
    }
    
    /**
     * Returns the current spelling corrector.
     * @return
     */
    private SpellingCorrector getSpellingCorrector() { 	
    	if(this.spellingCorrector == null) {
    		this.spellingCorrector = new SpellingCorrector(
    				this.getLevenshteinCalculator(), 
    				this.invertedIndexReader);
    	}
    	
    	return this.spellingCorrector;
    }
    
    /**
     * Returns the current content lookup.
     * @return
     */
    private PatentContentLookup getPatentContentLookup() {
    	if(this.patentContentLookup == null) {
    		this.patentContentLookup = new PatentContentLookup(this.dataDirectoryPath);
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
    				this.getTextPreprocessor(),
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
    public void index() {
    	this.index(false);
    }

    @Override
    public boolean loadIndex() {
    	return this.loadIndex(false);
    }
    
    @Override
    public void compressIndex() {
    	this.index(true);
    }

    @Override
    boolean loadCompressedIndex() {
    	return this.loadIndex(true);
    }
    
    @Override
    public ArrayList<String> search(String query, int topK) {
    	if(this.isLoaded) {    	
	    	try {
	    		RankedQueryResult result = this.getQueryProcessor().search(query, topK);	    		
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
	public Double computeNdcg(ArrayList<String> goldRanking, ArrayList<String> results, int p) {
		return this.getNcdgCalculator().calculate(goldRanking, results, p);
	}
	
    
    /**
     * Indexes all documents of a given directory. Argument compress determines, whether the index should be compressed.
     * @param documentDirectory
     * @param compress
     */
    private void index(boolean compress) {
    	try {
        	// Get xml files inside given directory
    		String[] documentFiles = Files.walk(this.dataDirectoryPath)
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
    		this.documentMapReader = new DocumentMapReader(this.documentMapFile, this.documentMapSeekListFile, compress);
    		this.citationIndexReader = new CitationIndexReader(this.citationIndexFile, this.citationIndexSeekListFile, compress);
    		this.invertedIndexReader = new InvertedIndexReader(this.indexFile, this.indexSeekListFile, compress);
    		this.spellingCorrector = new SpellingCorrector(this.getLevenshteinCalculator(), this.invertedIndexReader);
		} catch (IOException e) {
			e.printStackTrace();
			this.isLoaded = false;
			return false;
		}
    	    	
    	this.isLoaded = true;
    	this.queryProcessor = null;
        
        return true;
    }
}
