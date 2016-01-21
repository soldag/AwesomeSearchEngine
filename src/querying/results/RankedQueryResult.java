package querying.results;

import java.util.List;
import java.util.Map;

import documents.PatentDocument;
import postings.PostingTable;

public class RankedQueryResult extends UnrankedQueryResult {

	protected final List<PatentDocument> rankedDocuments;
	
	
	public RankedQueryResult(PostingTable tokenPostings, Map<String, String> spellingCorrections, List<PatentDocument> rankedDocumentMap) {
		super(tokenPostings, spellingCorrections);
		this.rankedDocuments = rankedDocumentMap;
	}
	
	
	public List<PatentDocument> getRankedDocuments() {
		return this.rankedDocuments;
	}
}
