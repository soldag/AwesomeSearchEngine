package indexing.documentmap;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import indexing.generic.GenericSeekList;
import io.index.IndexReader;
import io.index.IndexWriter;

public class DocumentMapSeekList extends GenericSeekList<Integer> {
	
	/**
	 * Determines, how many documents are skipped in between two seek list entries.
	 */
	private static final int SKIP_NUMBER = 200;
	
	
	/**
	 * Creates a new DocumentMapSeekList instance.
	 */
	public DocumentMapSeekList() {
		super(SKIP_NUMBER);
	}
	
	
	/**
	 * Loads the seek list from a specified FileReader.
	 */
	public void load(IndexReader reader) throws IOException {
		this.clear();
		
		while(reader.getFilePointer() < reader.length()) {
			int documentId = reader.readInt();
			int offset = reader.readInt();
			
			this.put(documentId, offset);
		}
	}
	
	/**
	 * Saves the seek list to a specified FileWriter.
	 */
	public void save(IndexWriter writer) throws IOException {
		// Sort document ids alphabetically
		List<Integer> documentIds = this.keySet().stream().sorted().collect(Collectors.toList());
				
		for(int documentId: documentIds) {
			writer.writeInt(documentId);
			writer.writeInt(this.get(documentId));
		}
	}
}
