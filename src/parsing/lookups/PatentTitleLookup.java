package parsing.lookups;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import parsing.PatentDocument;

public class PatentTitleLookup extends PatentPropertyLookup {
	
	/**
	 * Creates a new PatentTitleLookup instance.
	 * @param documentDirectory
	 */
	public PatentTitleLookup(Path documentDirectory) {
		super(documentDirectory);
	}

	
	/**
	 * Gets the title of the given patent document.
	 */
	@Override
	public String get(PatentDocument document) throws FileNotFoundException, IOException {
		return this.readFromFile(document.getFileId(), document.getTitleOffset(), document.getTitleLength());
	}	
}
