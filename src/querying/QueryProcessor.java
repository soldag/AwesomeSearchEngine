package querying;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import SearchEngine.PatentDocument;
import SearchEngine.Posting;
import indexing.DocumentMapReader;
import indexing.DocumentMapSeekList;
import indexing.InvertedIndexReader;
import indexing.InvertedIndexSeekList;
import textprocessing.TextPreprocessor;

public class QueryProcessor {
	
	/**
	 * Contains a list of supported boolean operators.
	 */
	private static final List<String> BOOLEAN_OPERATORS = Arrays.asList("AND", "OR", "NOT");
	
	/**
	 * Contains necessary services.
	 */
	private TextPreprocessor textPreprocessor;
	private DocumentRanker documentRanker;
	private DamerauLevenshteinCalculator damerauLevenshtein;
	private SpellingCorrector spellingCorrector;
	
	/**
	 * Contain necessary index files and reader services.
	 */
	private File documentMapFile;
	private DocumentMapReader documentMapReader;
	private File documentMapSeekListFile;
	private DocumentMapSeekList documentMapSeekList;

	private File indexFile;
	private InvertedIndexReader indexReader;
	private File indexSeekListFile;
	private InvertedIndexSeekList indexSeekList;
	
	/**
	 * Determines, whether the index is compressed, or not.
	 */
	private boolean compressed;
	
	/**
	 * Determines, whether the index has been loaded and the instance is ready for querying.
	 */
	private boolean isReady = false;
	
	/**
	 * Creates a new QueryProcessor instance.
	 * @param textProcessor
	 * @param damerauLevenshtein
	 * @param documentRanker
	 * @param documentMapFile
	 * @param documentMapSeekListFile
	 * @param indexFile
	 * @param indexSeekListFile
	 * @param compressed
	 * @throws FileNotFoundException
	 */
	public QueryProcessor(TextPreprocessor textProcessor, DamerauLevenshteinCalculator damerauLevenshtein, DocumentRanker documentRanker, 
			File documentMapFile, File documentMapSeekListFile, File indexFile, File indexSeekListFile, 
			boolean compressed) throws FileNotFoundException {
		this.textPreprocessor = textProcessor;
		this.damerauLevenshtein = damerauLevenshtein;
		this.documentRanker = documentRanker;
		
		this.documentMapFile = documentMapFile;
		this.documentMapSeekListFile = documentMapSeekListFile;
		this.documentMapSeekList = new DocumentMapSeekList();
		
		this.indexFile = indexFile;
		this.indexSeekListFile = indexSeekListFile;
		this.indexSeekList = new InvertedIndexSeekList();		
		this.compressed = compressed;
	}

	
	/**
	 * Gets a boolean, that determines, whether the index has been loaded and the instance is ready for querying.
	 * @return
	 */
	public boolean isReady() {
		return this.isReady;
	}
	
	/**
	 * Loads the seek lists from disk to memory in order to perform queries.
	 * @throws IOException
	 */
	public void load() throws IOException {
		this.documentMapReader = new DocumentMapReader(documentMapFile);
		try(RandomAccessFile seekListReader = new RandomAccessFile(this.documentMapSeekListFile, "r")) {
			this.documentMapSeekList.load(seekListReader);
		}
		
		this.indexReader = new InvertedIndexReader(this.indexFile, this.compressed);
		try(RandomAccessFile seekListReader = new RandomAccessFile(this.indexSeekListFile, "r")) {
			this.indexSeekList.load(seekListReader);
		}

		this.spellingCorrector = new SpellingCorrector(this.damerauLevenshtein, this.indexReader, this.indexSeekList);
		
		this.isReady = true;
	}
	
	/**
	 * Search for a given query in the document collection.
	 * @param query
	 * @param topK
	 * @return List of patent documents
	 * @throws IOException
	 */
	public List<PatentDocument> search(String query, int topK, int prf) throws IOException {
		String mode = "";
		List<String> tokens = this.textPreprocessor.tokenize(query, true);
		String booleanOperator = this.getBooleanOperator(tokens);
		if(booleanOperator != null) {
			tokens.remove(booleanOperator);
			mode = booleanOperator.toUpperCase();
		}
		else if(query.startsWith("'") && query.endsWith("'")) {
			mode = "PHRASE";
		}
		
		tokens = this.textPreprocessor.removeStopWords(tokens);
		
		switch(mode) {
			case "AND":
				return this.searchAnd(tokens);
				
			case "OR":
				return this.searchOr(tokens);
				
			case "NOT":
				return this.searchNot(tokens);
				
			case "PHRASE":
				return this.searchPhrase(tokens, topK);
				
			default:
				return this.searchDefault(tokens, topK, prf);
		}
	}
	
	/**
	 * Extracts the boolean operator from a tokenized query. If no operator is present, null is returned.
	 * @param tokens
	 * @return
	 */
	private String getBooleanOperator(List<String> tokens) {
		if(tokens.size() == 3) {
			String operator = tokens.get(1);
			if(BOOLEAN_OPERATORS.contains(operator.toUpperCase())) {
				return operator;
			}
		}
		
		return null;
	}
	
	/**
	 * Search for conjunction of given tokens in the document collection.
	 * @param tokens
	 * @return List of patent documents.
	 * @throws IOException
	 */
	private List<PatentDocument> searchAnd(List<String> tokens) throws IOException {
		List<Integer> result1 = this.getDocumentIds(this.searchToken(tokens.get(0)));
		List<Integer> result2 = this.getDocumentIds(this.searchToken(tokens.get(1)));
		
		return this.getDocuments(result1.stream()
				.filter(x -> result2.contains(x))
				.collect(Collectors.toList()));
	}
	
	/**
	 * Search for disjunction of given tokens in the document collection.
	 * @param tokens
	 * @return List of patent documents.
	 * @throws IOException
	 */
	private List<PatentDocument> searchOr(List<String> tokens) throws IOException {
		return this.getDocuments(this.getDocumentIds(tokens.stream()
				.map(x -> this.searchToken(x))
				.flatMap(x -> x.stream())
				.collect(Collectors.toList())));
	}
	
	/**
	 * Search for negated conjunction of given tokens in the document collection.
	 * @param tokens
	 * @return List of patent documents.
	 * @throws IOException
	 */
	private List<PatentDocument> searchNot(List<String> tokens) throws IOException {
		List<Integer> result1 = this.getDocumentIds(this.searchToken(tokens.get(0)));
		List<Integer> result2 = this.getDocumentIds(this.searchToken(tokens.get(1)));
		
		return this.getDocuments(result1.stream()
				.filter(x -> !result2.contains(x))
				.collect(Collectors.toList()));
	}
	
	/**
	 * Searched for the phrase of given tokens in the document collection. Only document, containing tokens in the given order are returned.
	 * @param tokens
	 * @param topK
	 * @return List of patent documents.
	 * @throws IOException
	 */
	private List<PatentDocument> searchPhrase(List<String> tokens, int topK) throws IOException {
		List<Posting> postings = null;
		Map<String, Integer> collectionFrequencies = new HashMap<String, Integer>();
		Map<String, Map<Integer, Integer>> documentFrequencies = new HashMap<String, Map<Integer, Integer>>();
		for(int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);

			List<Posting> newPostings = this.searchToken(token);
			for(Posting posting: newPostings) {
				int documentId = posting.getDocumentId();
				int positionsCount = posting.getPositions().length;
				if(!documentFrequencies.containsKey(token)) {
					documentFrequencies.put(token, new HashMap<Integer, Integer>());
				}
				if(documentFrequencies.get(token).containsKey(documentId)) {
					documentFrequencies.get(token).put(documentId, documentFrequencies.get(token).get(documentId) + positionsCount);
				}
				else {
					documentFrequencies.get(token).put(documentId, positionsCount);
				}
			}
			collectionFrequencies.put(token, newPostings.stream().mapToInt(x -> x.getPositions().length).sum());
			
			if(postings == null) {
				postings = newPostings;
			}
			else {
				List<Posting> oldPostings = postings;
				postings = newPostings.stream()
							.filter(newPosting -> oldPostings.stream().anyMatch(oldPosting -> this.areSuccessive(oldPosting, newPosting)))
							.collect(Collectors.toList());
			}
		}
		
		// Weight documents
		List<PatentDocument> result = this.getDocuments(postings.stream().map(x -> x.getDocumentId()).collect(Collectors.toList()));
		result = this.documentRanker.weightResult(result, documentFrequencies, collectionFrequencies, this.indexReader.getTotalTokenCount(), topK);
		
		return result;
	}
	
	/**
	 * Searched "normally" for a given query. Results of query tokens are combined(disjunction) and weighted.
	 * @param tokens
	 * @param topK
	 * @param prf
	 * @return List of patent documents.
	 * @throws IOException 
	 */
	private List<PatentDocument> searchDefault(List<String> tokens, int topK, int prf) throws IOException {
		List<PatentDocument> result = this.simpleSearch(tokens, topK);
		
		// Extend query by most frequent tokens of top documents (pseudo relevance)
		if(prf > 0) {
			List<String> additionalTerms = result.stream()
											.limit(prf)
											.flatMap(x -> x.getMostFrequentTokens().stream())
											.collect(Collectors.toList());
			tokens.addAll(additionalTerms);
			tokens = tokens.stream().distinct().collect(Collectors.toList());
			result = this.simpleSearch(tokens, topK);
		}
		
		return result;
	}
	
	/**
	 * Runs a simple search for a list of query tokens and weights the result.
	 * @param tokens
	 * @param topK
	 * @return List of patent documents.
	 * @throws IOException
	 */
	private List<PatentDocument> simpleSearch(List<String> tokens, int topK) throws IOException {
		List<Integer> documentIds = new ArrayList<Integer>();
		Map<String, Integer> collectionFrequencies = new HashMap<String, Integer>();
		Map<String, Map<Integer, Integer>> documentFrequencies = new HashMap<String, Map<Integer, Integer>>();
		for(int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);

			List<Posting> resultPostings = this.searchToken(token);
			for(Posting posting: resultPostings) {
				int documentId = posting.getDocumentId();
				int positionsCount = posting.getPositions().length;
				if(!documentFrequencies.containsKey(token)) {
					documentFrequencies.put(token, new HashMap<Integer, Integer>());
				}
				if(documentFrequencies.get(token).containsKey(documentId)) {
					documentFrequencies.get(token).put(documentId, documentFrequencies.get(token).get(documentId) + positionsCount);
				}
				else {
					documentFrequencies.get(token).put(documentId, positionsCount);
				}
			}
			collectionFrequencies.put(token, resultPostings.stream().mapToInt(x -> x.getPositions().length).sum());
			documentIds.addAll(resultPostings.stream().map(x -> x.getDocumentId()).collect(Collectors.toList()));
		}
		
		// Remove duplicates
		documentIds = documentIds.stream().distinct().collect(Collectors.toList());
		
		// Weight documents
		List<PatentDocument> result = this.getDocuments(documentIds);
		result = this.documentRanker.weightResult(result, documentFrequencies, collectionFrequencies, this.indexReader.getTotalTokenCount(), topK);
		
		return result;
	}
	
	/**
	 * Determines, whether posting1 is successor of posting2 in the very same document.
	 * @param posting1
	 * @param posting2
	 * @return boolean
	 */
	private boolean areSuccessive(Posting posting1, Posting posting2) {
		if(posting1.getDocumentId() == posting2.getDocumentId()) {
			return Arrays.stream(posting1.getPositions())
						.anyMatch(x -> Arrays.stream(posting2.getPositions())
											.anyMatch(y -> y == x + 1));
		}
		
		return false;
	}
	
	/**
	 * Gets all postings of a given token from index.
	 * @param token
	 * @return List of postings.
	 */
	private List<Posting> searchToken(String token) {
		try {
			boolean prefixSearch = token.endsWith("*");
			if(prefixSearch) {
				token = token.substring(0, token.length() - 1);
			}
			else {
				token = this.textPreprocessor.stem(token);
			}
			
			int startOffset = this.indexSeekList.getIndexOffset(token);
			List<Posting> result = this.indexReader.getPostingsList(token, startOffset, prefixSearch);
			
			// Spelling correction
			if(!prefixSearch && result.isEmpty()) {
				String correctedToken = this.spellingCorrector.correctToken(token); 
				if(correctedToken != null) {
					result = searchToken(correctedToken);
					System.out.println("We changed your query term due to a (possible) spelling error from " + token + " to " + correctedToken + "!");
				}
			}
			return result;
		}
		catch(IOException e) {
			return new ArrayList<Posting>();
		}
	}


	/**
	 * Extracts distinct document ids from a list of postings
	 * @param postings
	 * @return List of document ids.
	 */
	private List<Integer> getDocumentIds(List<Posting> postings) {
		return postings.stream()
				.mapToInt(x -> x.getDocumentId())
				.distinct()
				.boxed()
				.collect(Collectors.toList());
	}
	
	/**
	 * Gets the PatentDocument instance for each of the given document ids.
	 * @param documentIds
	 * @return List of PatentDocument instances.
	 * @throws IOException
	 */
	private List<PatentDocument> getDocuments(List<Integer> documentIds) throws IOException {
		return documentIds.stream()
				.map(this::getDocument)
				.collect(Collectors.toList());
	}
	
	/**
	 * Retrieves the PatentDocument from document map by specifying its id.
	 * @param documentId
	 * @return
	 */
	private PatentDocument getDocument(int documentId) {
		int startOffset = this.documentMapSeekList.getIndexOffset(documentId);
		try {
			return this.documentMapReader.getDocument(documentId, startOffset);
		} catch (IOException e) {
			return null;
		}
	}
}
