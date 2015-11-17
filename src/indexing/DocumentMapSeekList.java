package indexing;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
	 * Loads the seek list from a specified DataInput.
	 */
	public void load(DataInput input) throws IOException {
		this.seekList.clear();
		
		while(true) {
			try {
				int documentId = input.readInt();
				int offset = input.readInt();
				
				this.seekList.put(documentId, offset);
			}
			catch(EOFException e) {
				break;
			}
		}
	}
	
	/**
	 * Saves the seek list to a specified DataOutput.
	 */
	public void save(DataOutput output) throws IOException {
		// Sort tokens alphabetically
		List<Integer> documentIds = this.seekList.keySet().stream().sorted().collect(Collectors.toList());
				
		for(int documentId: documentIds) {
			output.writeInt(documentId);
			output.writeInt(this.seekList.get(documentId));
		}
	}
}
