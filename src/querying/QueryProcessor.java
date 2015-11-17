package querying;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	 * Contains the lambda factor for jelinek-mercer smoothing.
	 */
	private static final double QL_LAMBDA = 0.2;

	
	/**
	 * Contains a text preprocessor instance.
	 */
	private TextPreprocessor textPreprocessor;
	
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
	 * @param documentMapFile
	 * @param documentMapSeekListFile
	 * @param indexFile
	 * @param indexSeekListFile
	 * @param compressed
	 * @throws FileNotFoundException
	 */
	public QueryProcessor(TextPreprocessor textProcessor, File documentMapFile, File documentMapSeekListFile, File indexFile, File indexSeekListFile, boolean compressed) throws FileNotFoundException {
		this.textPreprocessor = textProcessor;
		
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
		
		this.isReady = true;
	}
	
	/**
	 * Search for a given query in the document collection.
	 * @param query
	 * @param topK
	 * @return List of patent documents
	 * @throws IOException
	 */
	public List<PatentDocument> search(String query, int topK) throws IOException {
		String mode = "";
		List<String> tokens = this.textPreprocessor.tokenize(query, true);
		String booleanOperator = this.getBooleanOperator(tokens);
		if(booleanOperator != null) {
			tokens.remove(booleanOperator);
			mode = booleanOperator.toUpperCase();
		}
		
		tokens = this.textPreprocessor.removeStopWords(tokens);
		
		switch(mode) {
			case "AND":
				return this.searchAnd(tokens);
				
			case "OR":
				return this.searchOr(tokens);
				
			case "NOT":
				return this.searchNot(tokens);
				
			default:
				return this.searchPhrase(tokens, topK);
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
		for(int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);

			List<Posting> resultPostings = this.searchToken(token);
			collectionFrequencies.put(token, resultPostings.stream().mapToInt(x -> x.getPositions().length).sum());
			
			if(postings == null) {
				postings = resultPostings;
			}
			else {
				postings = postings.stream()
						.filter(posting1 -> resultPostings.stream().anyMatch(posting2 -> this.areSuccessive(posting1, posting2)))
						.collect(Collectors.toList());
			}
		}
		
		// Weight documents
		List<PatentDocument> result = this.weightResult(postings, tokens, collectionFrequencies, topK);
		
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
	 * Weights a list of patent document for a given query using query-likelihood-model. Resulting list is limited to topK entries.
	 * @param postings
	 * @param tokens
	 * @param collectionFrequencies
	 * @param topK
	 * @return List of ranked patent documents limited to topK.
	 * @throws IOException
	 */
	private List<PatentDocument> weightResult(List<Posting> postings, List<String> tokens, Map<String, Integer> collectionFrequencies, int topK) throws IOException {
		HashMap<PatentDocument, Double> weightedDocuments = new HashMap<PatentDocument, Double>();
		for(Posting posting: postings) {
			PatentDocument document = this.getDocument(posting.getDocumentId());
			if(document != null) {
				double weight = Math.log(tokens.stream()
										.mapToDouble(x -> this.queryLikelihood(
																posting.getPositions().length, 
																document.getTokensCount(), 
																collectionFrequencies.get(x), 
																this.indexReader.getTotalTokenCount()))
										.reduce(1, (x,y) -> x*y));
				weightedDocuments.put(document, weight);
			}
		}
		
		// Sort result by weight descending and limit results by topK
		return weightedDocuments.entrySet().stream()
				.sorted(Collections.reverseOrder(new MapValueComparator()))
				.limit(topK)
				.map(x -> x.getKey())
				.collect(Collectors.toList());
	}
	
	/**
	 * Calculates the query-likelihood-ranking for a specific token.
	 * @param tokenDocumentFrequency
	 * @param documentsLength
	 * @param tokenCollectionFrequency
	 * @param collectionLength
	 * @return
	 */
	private double queryLikelihood(int tokenDocumentFrequency, int documentsLength, int tokenCollectionFrequency, int collectionLength) {
		return (1 - QL_LAMBDA) * ((double)tokenDocumentFrequency / (double)documentsLength) + QL_LAMBDA * ((double)tokenCollectionFrequency / (double)collectionLength);
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
			return this.indexReader.getPostings(token, startOffset, prefixSearch);
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
