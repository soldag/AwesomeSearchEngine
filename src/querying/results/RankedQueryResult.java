package querying.results;

import java.util.List;
import java.util.Map;
import java.util.Set;

import documents.PatentDocument;
import postings.PostingTable;

public class RankedQueryResult implements QueryResult {
	
	/**
	 * Contains the found postings per token.
	 */
	private final PostingTable tokenPostings;
	
	/**
	 * Contains a list of documents that match a LinkTo-query.
	 */
	private final Set<Integer> linkingDocuments;
	
	/**
	 * Contains the map of spelling corrections. Key is the original token, value the corrected one.
	 */
	private final Map<String, String> spellingCorrections;

	/**
	 * Contains the ordered list of ranked documents.
	 */
	private final List<PatentDocument> rankedDocuments;
	
	
	/**
	 * Creates a new RankedQueryResult instance.
	 * @param tokenPostings
	 * @param linkingDocuments
	 * @param spellingCorrections
	 * @param rankedDocumentMap
	 */
	public RankedQueryResult(PostingTable tokenPostings, Set<Integer> linkingDocuments, Map<String, String> spellingCorrections, List<PatentDocument> rankedDocumentMap) {
		this.tokenPostings = tokenPostings;
		this.linkingDocuments = linkingDocuments;
		this.spellingCorrections = spellingCorrections;
		this.rankedDocuments = rankedDocumentMap;
	}
	
	
	@Override
	public PostingTable getPostings() {
		return tokenPostings;
	}

	@Override
	public Set<Integer> getLinkingDocuments() {
		return this.linkingDocuments;
	}

	@Override
	public Map<String, String> getSpellingCorrections() {
		return spellingCorrections;
	}	
	
	/**
	 * Gets the ordered list of ranked documents.
	 * @return
	 */
	public List<PatentDocument> getRankedDocuments() {
		return this.rankedDocuments;
	}
}
