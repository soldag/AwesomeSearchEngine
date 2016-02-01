package querying.results;

import java.util.List;
import java.util.Map;
import java.util.Set;

import documents.PatentDocument;
import postings.PostingTable;

public class RankedQueryResult extends UnrankedQueryResult {

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
		super(tokenPostings, linkingDocuments, spellingCorrections);
		this.rankedDocuments = rankedDocumentMap;
	}
	
	
	/**
	 * Gets the ordered list of ranked documents.
	 * @return
	 */
	public List<PatentDocument> getRankedDocuments() {
		return this.rankedDocuments;
	}
}
