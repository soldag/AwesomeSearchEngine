package parsing;

import java.io.IOException;
import java.util.EnumMap;


import documents.PatentContentDocument;
import documents.PatentDocument;
import indexing.contents.ContentsIndexReader;
import postings.ContentType;

public class PatentContentLookup {
	
	private final ContentsIndexReader indexReader;
	
	
	/**
	 * Creates a new PatentContentLookup instance.
	 * @param indexReader
	 */
	public PatentContentLookup(ContentsIndexReader indexReader) {
		this.indexReader = indexReader;
	}
	
	
	/**
	 * Loads the content of the given document.
	 * @param document
	 * @return
	 * @throws IOException
	 */
	public PatentContentDocument loadContent(PatentDocument document) throws IOException {
		EnumMap<ContentType, String> contents = this.indexReader.getContents(document.getId());
		if(contents != null) {
			return new PatentContentDocument(document, contents);
		}
		
		return null;
	}
}
