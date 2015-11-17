package indexing;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class InvertedIndexSeekList extends GenericSeekList<String> {
	
	/**
	 * Determines, how many documents are skipped in between two seek list entries.
	 */
	private static final int SKIP_NUMBER = 200;
	
	
	/**
	 * Creates a new IndexSeekList instance.
	 */
	public InvertedIndexSeekList() {
		super(SKIP_NUMBER);
	}
	
	
	/**
	 * Loads the seek list from a specified DataInput.
	 */
	public void load(DataInput input) throws IOException {
		this.seekList.clear();
		
		while(true) {
			try {
				String token = Token.read(input);
				int offset = input.readInt();				
				this.seekList.put(token, offset);
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
		List<String> tokens = this.seekList.keySet().stream().sorted().collect(Collectors.toList());
		
		for(String token: tokens) {
			Token.write(token, output);
			output.writeInt(this.seekList.get(token));
		}
	}
}
