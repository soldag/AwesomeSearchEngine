package querying.ranking;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import documents.PatentDocument;
import indexing.documentmap.DocumentMapReader;
import postings.ContentType;
import postings.DocumentPostings;
import postings.PostingTable;
import postings.positions.PositionMap;
import querying.results.QueryResult;
import querying.results.RankedQueryResult;
import querying.results.UnrankedQueryResult;
import utilities.MapValueComparator;

public class DocumentRanker {
	
	/**
	 * Contains the lambda factor for jelinek-mercer smoothing.
	 */
	private static final double QL_LAMBDA = 0.2;
	
	/**
	 * Contains a factor for weights of tokens, that are not part of the original query (when using prf)
	 */
	private static final double NON_QUERY_TOKEN_FACTOR = 0.25;
	
	/**
	 * Contains the document map reader service.
	 */
	private DocumentMapReader documentMapReader;
	
	
	/**
	 * Creates a new DocumentRanker instance.
	 * @param documentMapReader
	 */
	public DocumentRanker(DocumentMapReader documentMapReader) {
		this.documentMapReader = documentMapReader;
	}

	
	/**
	 * Weights the given query result using query-likelihoof and page rank measures.
	 * @param result
	 * @param resultLimit
	 * @param collectionTokenCount
	 * @return
	 */
	public RankedQueryResult weightResult(UnrankedQueryResult result, int resultLimit, int collectionTokenCount) {
		PostingTable resultPostings = result.getPostings();
		
		// Narrow range of document to the ones, which contain most of the query tokens
		Set<Integer> rankingDocumentIds = resultPostings.documentIdSet();
		if(rankingDocumentIds.size() > resultLimit) {
			int i = resultPostings.tokenSet().size();
			rankingDocumentIds = new HashSet<Integer>();
			while(rankingDocumentIds.size() < resultLimit && i > 0)
			{
				final int count = i;
				rankingDocumentIds.addAll(resultPostings.documentIdSet().stream()
													.filter(document -> resultPostings.ofDocument(document).entrySet().stream()
																			.filter(x -> x.getValue().size() > 0).count() == count)
													.collect(Collectors.toSet()));
				i--;
			}
		}
		rankingDocumentIds = Sets.union(rankingDocumentIds, result.getLinkingDocuments());
		
		// Calculate weights for each document
		Map<PatentDocument, Double> weights = rankingDocumentIds.stream()
													.map(this::loadDocument)
													.filter(Objects::nonNull)
													.collect(Collectors.toMap(
															Function.identity(), 
															document -> this.weightDocument(document, result, collectionTokenCount)));
		
		// Sort documents by weight and limit to given parameter
		List<PatentDocument> rankedDocuments = weights.entrySet().stream()
													.sorted(MapValueComparator.reverse())
													.limit(resultLimit)
													.map(e -> e.getKey())
													.collect(Collectors.toList());
		
		return this.buildRankedResult(result, rankedDocuments);
	}
	
	/**
	 * Limit given query result to given number. The most recent documents are returned.
	 * @param result
	 * @param resultLimit
	 * @return
	 */
	public RankedQueryResult limitResult(UnrankedQueryResult result, int resultLimit) {
		List<PatentDocument> documents = Sets.union(result.getPostings().documentIdSet(), result.getLinkingDocuments()).stream()
												.sorted(Collections.reverseOrder())
												.limit(resultLimit)
												.map(this::loadDocument)
												.filter(Objects::nonNull)
												.collect(Collectors.toList());
		
		return this.buildRankedResult(result, documents);
	}
	
	
	/**
	 * Loads the specified document from document map.
	 * @param documentId
	 * @return
	 */
	private PatentDocument loadDocument(int documentId) {
		try {
			return this.documentMapReader.getDocument(documentId);
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Calculates the query-likelihood-weight for a given document.
	 * @param document
	 * @param tokenPostings
	 * @param result
	 * @param collectionTokenCount
	 * @return
	 */	
	private double weightDocument(PatentDocument document, UnrankedQueryResult result, int collectionTokenCount) {
		// Calculate weight of tokens
		double tokenWeight = Arrays.stream(ContentType.values())
								.mapToDouble(contentType -> contentType.getWeightingFactor() * 
															this.weightDocumentByTokens(document, contentType, result, collectionTokenCount))
								.sum();		
		
		return tokenWeight;
	}
	
	/**
	 * Calculates the query-likelihood-weight for a specific content type of a given document.
	 * @param document
	 * @param tokenPostings
	 * @param result
	 * @param collectionTokenCount
	 * @return
	 */	
	private double weightDocumentByTokens(PatentDocument document, ContentType contentType, UnrankedQueryResult result, int collectionTokenCount) {
		DocumentPostings documentPostings = result.getPostings().ofDocument(document.getId());
		return result.getPostings().tokenSet().stream()
						.mapToDouble(token -> this.getPrfFactor(token, result) * 
											  this.queryLikelihood(
												this.countTokenOccurrences(token, documentPostings, contentType), 
												document.getTokensCount(contentType), 
												result.getCollectionFrequencies().get(token), 
												collectionTokenCount))
						.reduce(1, (x,y) -> x*y);
	}
	
	/**
	 * Returns a factor for weighting of tokens.
	 * @param token
	 * @param result
	 * @return
	 */
	private double getPrfFactor(String token, UnrankedQueryResult result) {
		if(result.hasOriginalResult()) {
			QueryResult prfResult = result.getOriginalResult();
			Set<String> originalQueryTokens = prfResult.getPostings().tokenSet();
			if(!originalQueryTokens.contains(token)) {
				return NON_QUERY_TOKEN_FACTOR;
			}
		}
		
		return 1;
	}
	
	/**
	 * Counts the number of token occurrences in the specified content of the given postings 
	 * @param token
	 * @param documentPostings
	 * @param contentType
	 * @return
	 */
	private int countTokenOccurrences(String token, DocumentPostings documentPostings, ContentType contentType) {
		PositionMap positionMap = documentPostings.ofToken(token);
		if(positionMap != null) {
			return positionMap.size(contentType);
		}
		
		return 0;
	}
	
	/**
	 * Calculates the query-likelihood-ranking for a specific token.
	 * @param tokenDocumentFrequency
	 * @param documentsLength
	 * @param tokenCollectionFrequency
	 * @param collectionLength
	 * @return
	 */
	private double queryLikelihood(double tokenDocumentFrequency, double documentsLength, double tokenCollectionFrequency, double collectionLength) {
		return Math.log1p((1 - QL_LAMBDA) * (tokenDocumentFrequency / documentsLength) + QL_LAMBDA * (tokenCollectionFrequency / collectionLength));
	}
	
	
	/**
	 * Creates a RankedQueryResult instance for the given result and order of ranked documents.
	 * @param result
	 * @param rankedDocuments
	 * @return
	 */
	private RankedQueryResult buildRankedResult(QueryResult result, List<PatentDocument> rankedDocuments) {
		PostingTable postingTable = new PostingTable();
		Set<Integer> linkingDocuments = new HashSet<Integer>();
		for(PatentDocument document: rankedDocuments) {
			// Postings
			DocumentPostings documentPostings = result.getPostings().ofDocument(document.getId());
			if(documentPostings != null) {
				postingTable.putAll(document.getId(), documentPostings);
			}
			
			// Linked documents
			if(result.getLinkingDocuments().contains(document.getId())) {
				linkingDocuments.add(document.getId());
			}
		}
		
		
		return new RankedQueryResult(postingTable, linkingDocuments, result.getSpellingCorrections(), rankedDocuments);
	}
}
