package SearchEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.javatuples.Pair;

public class AwesomeQueryProcessor {

	// XPaths constants for XML elements containing the document ID and title of a patent.
	private static final String DOCUMENT_ID_XPATH = "my-root/us-patent-grant/us-bibliographic-data-grant/publication-reference/document-id/doc-number";
	private static final String TITLE_XPATH = "my-root/us-patent-grant/us-bibliographic-data-grant/invention-title";

	// Contain instances of necessary services
	private AwesomeTextProcessor textProcessor;

	// Contain various index files
	private File indexFile;
	private File seekListFile;
	private File documentMapFile;
	
	private LinkedHashMap<String, Long> seekList = new LinkedHashMap<String, Long>();
	private HashMap<String, String> documentMap = new HashMap<String, String>();
	
	
	public AwesomeQueryProcessor(AwesomeTextProcessor textProcessor, File indexFile, File seekListFile, File documentMapFile) {
		this.textProcessor = textProcessor;

		this.indexFile = indexFile;
		this.seekListFile = seekListFile;
		this.documentMapFile = documentMapFile;
	}
	
	
	// Loads parts of the index from file into main memory.
	public void loadIndex() throws FileNotFoundException {
		this.loadSeekList();
		this.loadDocumentMap();
	}
	
	// Loads complete seek list from file into main memory.
	private void loadSeekList() throws FileNotFoundException {
		try (Scanner seekListScanner = new Scanner(new FileInputStream(this.seekListFile), "UTF-8")) {
			while(seekListScanner.hasNextLine()) {
				String line = seekListScanner.nextLine();
				
				String[] splittedLine = line.split(Pattern.quote(AwesomeIndexer.KEY_VALUE_SEPARATOR));
				String token = splittedLine[0];
				long offset = Long.parseLong(splittedLine[1]);
				
				this.seekList.put(token, offset);
			}
		}
	}
	
	// Loads complete document map from file into main memory.
	private void loadDocumentMap() throws FileNotFoundException {
		try (Scanner documentMapScanner = new Scanner(new FileInputStream(this.documentMapFile), "UTF-8")) {
			while(documentMapScanner.hasNextLine()) {
				String line = documentMapScanner.nextLine();
				
				String[] splittedLine = line.split(Pattern.quote(AwesomeIndexer.KEY_VALUE_SEPARATOR));
				this.documentMap.put(splittedLine[0], splittedLine[1]);
			}
		}
	}
	
	// Performs a single word query.
	public ArrayList<String> search(String query) throws IOException, XMLStreamException {
		// Tokenize(including stemming and stop-word-removal) query
		String[] queryTokens = this.textProcessor.getTokens(query).stream().map(x -> x.getValue0()).toArray(String[]::new);
		String queryToken = queryTokens[0]; // For now only the first word
		
		Pair<Long, Long> indexOffsets = this.getIndexOffsets(queryToken);
		Long startOffset = indexOffsets.getValue0();
		Long endOffset = indexOffsets.getValue1();
		
		if(indexOffsets != null) {
			try (RandomAccessFile indexFileReader = new RandomAccessFile(this.indexFile, "r")) {
				// Correct end offset, if necessary ('null' means, read file to end)
				if(endOffset == null) {
					endOffset = indexFileReader.length();
				}
				
				// Move file pointer to start offset
				indexFileReader.seek(startOffset);
				
				// Read line by line, until end offset is reached
				while(indexFileReader.getFilePointer() < endOffset) {
					String line = indexFileReader.readLine();
					
					// Extract token from complete entry
					int index = line.indexOf(AwesomeIndexer.KEY_VALUE_SEPARATOR);
					String token = line.substring(0, index);
					
					// If read token matches query token, get and return list of titles for the corresponding document IDs
					if(token.equals(queryToken)) {
						// Extract distinct document IDs from serialized posting lists
						String serializedPostingList = line.substring(index + 1);
						String[] serializedPostings = serializedPostingList.split(AwesomeIndexer.POSTINGS_SEPARATOR);
						String[] documentIds = Arrays.stream(serializedPostings)
												.map(x -> x.substring(1, x.indexOf(AwesomeIndexer.POSTING_ENTRY_SEPARATOR)))
												.distinct()
												.toArray(String[]::new);
						
						// Return titles for the corresponding document IDs
						return this.getDocumentTitles(documentIds);
					}
				}
			}
		}
		
		return new ArrayList<String>();
	}
	
	// Gets start and end offset for the part of the index file, which could contain the token using the seek list. 
	// If null is returned, token is not part of the index.
	// If end offset is null, the index has to be read to end.
	private Pair<Long, Long> getIndexOffsets(String token) {
		// Direct match: seek list contains token
		if(this.seekList.containsKey(token)) {
			long startOffset = this.seekList.get(token);
			return new Pair<Long, Long>(startOffset, startOffset + 1); // Little bit hacky ;D
		}
		
		// Iterate over seek list and determine the nearest predecessor and successor of the token
		Long startOffset = null;
		for(Map.Entry<String, Long> entry: this.seekList.entrySet()) {
			if(token.compareTo(entry.getKey()) < 0) {
				if(startOffset != null) {
					long endOffset = entry.getValue();
					return new Pair<Long, Long>(startOffset, endOffset);
				}
				else {
					// Already the first token in the seek list (which is also the first one in the index) is bigger, than the token searched-for.
					// This means, the index does not contain this token.
					return null;
				}
			}
			startOffset = entry.getValue();
		}
		
		// Token could only be appear after the last token of the seek list in index file, thus no end offset can be specified.
		return new Pair<Long, Long>(startOffset, null);
	}
	
	// Extracts the titles of documents identified by its IDs.
	private ArrayList<String> getDocumentTitles(String[] documentIds) throws FileNotFoundException, XMLStreamException {
		ArrayList<String> titles = new ArrayList<String>();
		
		// Construct a inverted document map for the documents searched-for, which maps each document file to a list of document IDs contained in it.
		HashMap<String, List<String>> invertedDocumentMap = new HashMap<String, List<String>>();
		for(String documentId: documentIds) {
			List<String> idList;
			String filePath = this.documentMap.get(documentId);
			if(invertedDocumentMap.containsKey(filePath)) {
				// Get posting list for given token
				idList = invertedDocumentMap.get(filePath);
			}
			else {
				// Create new, empty posting list for given file path
				idList = new ArrayList<String>();
				invertedDocumentMap.put(filePath, idList);
			}

			// Add current document ID to list
			idList.add(documentId);
		}
		
		// Parse each document file and extract the titles of those documents searched-for.
		for(Map.Entry<String, List<String>> entry: invertedDocumentMap.entrySet()) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader parser = factory.createXMLStreamReader(new FileInputStream(entry.getKey()));
			
			String currentXPath = "";
			String currentDocumentId = "";
			while (parser.hasNext())
			{
				int eventType = parser.getEventType();
				if(eventType == XMLStreamReader.START_ELEMENT) {
					// Update current path
					if(currentXPath != null && !currentXPath.isEmpty()) 
			    	{
			        	currentXPath += "/";
			    	}
					currentXPath += parser.getLocalName();
				}
				else if(eventType == XMLStreamReader.END_ELEMENT) {
					// Update current path
					int index = currentXPath.lastIndexOf("/");
			    	if(index == -1) 
			    	{
			    		currentXPath = "";
			    	}
			    	else
			    	{
			    		currentXPath = currentXPath.substring(0, index);
			    	}
				}
				else if (eventType == XMLStreamReader.CHARACTERS) {
					if(currentXPath.equals(DOCUMENT_ID_XPATH)) {
						// Extract current document ID
						currentDocumentId = parser.getText();
			    	}
					// If the current element contains the title of the patent and the document ID is searched-for, add title to result list.
			    	else if(currentXPath.equals(TITLE_XPATH) && entry.getValue().contains(currentDocumentId))
			    	{
			    		titles.add(parser.getText());
			    	}
				}
				
				// Read next element
				parser.next();
			}
		}
		
		return titles;
	}
}
