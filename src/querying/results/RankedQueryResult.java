package querying.results;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;

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
	 * @param linkedDocuments
	 * @param spellingCorrections
	 * @param rankedDocumentMap
	 */
	public RankedQueryResult(PostingTable tokenPostings, Multimap<Integer, Integer> linkedDocuments, Map<String, String> spellingCorrections, List<PatentDocument> rankedDocumentMap) {
		super(tokenPostings, linkedDocuments, spellingCorrections);
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
