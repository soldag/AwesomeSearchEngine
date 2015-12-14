package indexing.documentmap;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import indexing.GenericSeekList;
import io.FileReader;
import io.FileWriter;

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
	public void load(FileReader reader) throws IOException {
		this.seekList.clear();
		
		while(reader.getFilePointer() < reader.length()) {
			int documentId = reader.readInt();
			int offset = reader.readInt();
			
			this.seekList.put(documentId, offset);
		}
	}
	
	/**
	 * Saves the seek list to a specified FileWriter.
	 */
	public void save(FileWriter writer) throws IOException {
		// Sort tokens alphabetically
		List<Integer> documentIds = this.seekList.keySet().stream().sorted().collect(Collectors.toList());
				
		for(int documentId: documentIds) {
			writer.writeInt(documentId);
			writer.writeInt(this.seekList.get(documentId));
		}
	}
}
