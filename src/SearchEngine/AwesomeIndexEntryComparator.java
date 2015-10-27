package SearchEngine;

import java.util.Comparator;

public class AwesomeIndexEntryComparator implements Comparator<String> {
	
	// Contains the character, that separates tokens from posting lists index.
	private String tokenSeparator;
	
	
	public AwesomeIndexEntryComparator(String tokenSeparator) {
		this.tokenSeparator = tokenSeparator;
	}

	
	@Override
	// Compares two index entries by comparing only the tokens.
	public int compare(String entry1, String entry2) {
		String token1 = this.getToken(entry1);
		String token2 = this.getToken(entry2);
		
		return token1.compareTo(token2);
	}
	
	// Extracts token out of a complete index entry.
	private String getToken(String entry) {
		int index = entry.indexOf(this.tokenSeparator);
		return entry.substring(0, index);
	}
}
