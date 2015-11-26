package parsing.lookups;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import parsing.PatentDocument;

public class PatentAbstractLookup extends AbstractPatentPropertyLookup {

	/**
	 * Creates a new PatentAbstractLookup instance.
	 * @param documentDirectory
	 */
	public PatentAbstractLookup(Path documentDirectory) {
		super(documentDirectory);
	}

	
	/**
	 * Gets the abstract of the given patent document.
	 */
	@Override
	public String get(PatentDocument document) throws FileNotFoundException, IOException {
		return this.readFromFile(document.getFileId(), document.getAbstractOffset(), document.getAbstractLength());
	}	
}
