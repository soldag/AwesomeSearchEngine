package querying;

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

import javax.xml.stream.XMLStreamException;

import org.javatuples.Pair;

import indexer.DocumentMapWriter;
import indexer.IndexWriter;
import indexer.SeekListWriter;
import parsers.PatentTitleLookup;
import textprocessing.AwesomeTextProcessor;

public class AwesomeQueryProcessor {

	// Contain instances of necessary services
	private AwesomeTextProcessor textProcessor;

	// Contain various index files
	private File indexFile;
	private File seekListFile;
	private File secondarySeekListFile;
	private File documentMapFile;
	
	private LinkedHashMap<String, Long> seekList = new LinkedHashMap<String, Long>();
	private HashMap<Integer, String> documentMap = new HashMap<Integer, String>();

	private boolean compressed;

	
	
	
	public AwesomeQueryProcessor(AwesomeTextProcessor textProcessor, File indexFile, File seekListFile, File secondarySeekListFile, File documentMapFile, boolean compressed) {
		this.textProcessor = textProcessor;

		this.indexFile = indexFile;
		this.seekListFile = seekListFile;
		this.secondarySeekListFile = secondarySeekListFile;
		this.documentMapFile = documentMapFile;
		this.compressed = compressed;
	}
	
	
	// Loads parts of the index from file into main memory.
	public void loadIndex() throws FileNotFoundException {
		if(this.compressed){
			this.loadSecondarySeekList();
		}
		else{
			this.loadSeekList();
		}			
		this.loadDocumentMap();
	}
	
	// Loads complete seek list from file into main memory.
	private void loadSeekList() throws FileNotFoundException {
		try (Scanner seekListScanner = new Scanner(new FileInputStream(this.seekListFile), "UTF-8")) {
			while(seekListScanner.hasNextLine()) {
				String line = seekListScanner.nextLine();
				if(!line.isEmpty()) {
					String[] splittedLine = line.split(Pattern.quote(SeekListWriter.SEPARATOR));
					String token = splittedLine[0];
					long offset = Long.parseLong(splittedLine[1].trim());
					this.seekList.put(token, offset);
				}
			}
		}
	}
	
	private void loadSecondarySeekList() throws FileNotFoundException{
		try (Scanner seekListScanner = new Scanner(new FileInputStream(this.secondarySeekListFile), "UTF-8")) {
			while(seekListScanner.hasNextLine()) {
				String line = seekListScanner.nextLine();
				if(!line.isEmpty()) {
					String[] splittedLine = line.split(Pattern.quote(SeekListWriter.SEPARATOR));
					String token = splittedLine[0];
					long offset = Long.parseLong(splittedLine[1].trim());
					this.seekList.put(token, offset);
				}
			}
		}
	}
	
	// Loads complete document map from file into main memory.
	private void loadDocumentMap() throws FileNotFoundException {
		try (Scanner documentMapScanner = new Scanner(new FileInputStream(this.documentMapFile), "UTF-8")) {
			while(documentMapScanner.hasNextLine()) {
				String line = documentMapScanner.nextLine();
				if(!line.isEmpty()){
					String[] splittedLine = line.split(Pattern.quote(DocumentMapWriter.SEPARATOR));
					this.documentMap.put(Integer.parseInt(splittedLine[0]), splittedLine[1]);
				}
			}
		}
	}
	
	// Performs a single word query.
	public ArrayList<String> search(String query) throws IOException, XMLStreamException {
		// Tokenize(including stemming and stop-word-removal) query
		String[] queryTokens = this.textProcessor.getTokens(query).stream().map(x -> x.getValue0()).toArray(String[]::new);
		String queryToken = queryTokens[0]; // For now only the first word	
		String seekListLine = null;
		
		if(this.compressed){
			Pair<Long, Long> indexOffsets = this.getIndexOffsets(queryToken);
			Long startOffset = indexOffsets.getValue0();
			Long endOffset = indexOffsets.getValue1();
			
			if(indexOffsets != null) {
				try (RandomAccessFile indexFileReader = new RandomAccessFile(this.seekListFile, "r")) {
					// Correct end offset, if necessary ('null' means, read file to end)
					if(endOffset == null) {
						endOffset = indexFileReader.length();
					}
					
					// Move file pointer to start offset
					indexFileReader.seek(startOffset);
					
					// Read line by line, until end offset is reached
					while(indexFileReader.getFilePointer() < endOffset) {
						String line = indexFileReader.readLine();
						String token = line.substring(0, line.indexOf(SeekListWriter.SEPARATOR));
						if(token.equals(queryToken)) {
							seekListLine = line;
							break;
						}
					}
				}
			}
		}
		else{
			try (Scanner seekListScanner = new Scanner(new FileInputStream(this.seekListFile), "UTF-8")) {
				while(seekListScanner.hasNextLine()) {
					String line = seekListScanner.nextLine();
					if(!line.isEmpty()){
						String token = line.substring(0, line.indexOf(SeekListWriter.SEPARATOR));
						if(token.equals(queryToken)) {
							seekListLine = line;
							break;
						}
					}
				}
			}
		}
		
		long offset = Long.parseLong(seekListLine.substring(seekListLine.indexOf(SeekListWriter.SEPARATOR) + 1));
		try(RandomAccessFile indexReader = new RandomAccessFile(this.indexFile, "r")) {
			indexReader.seek(offset);
			String line = indexReader.readLine();
			if(this.compressed){
				return processCompressedLine(line, queryToken);
			}
			else {
				return processLine(line, queryToken);
			}	
		}
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
	private ArrayList<String> getDocumentTitles(Integer[] documentIds) throws FileNotFoundException, XMLStreamException {		
		// Construct a inverted document map for the documents searched-for, which maps each document file to a list of document IDs contained in it.
		HashMap<String, List<Integer>> invertedDocumentMap = new HashMap<String, List<Integer>>();
		for(Integer documentId: documentIds) {
			List<Integer> idList;
			String filePath = this.documentMap.get(documentId);
			if(invertedDocumentMap.containsKey(filePath)) {
				// Get posting list for given token
				idList = invertedDocumentMap.get(filePath);
			}
			else {
				// Create new, empty posting list for given file path
				idList = new ArrayList<Integer>();
				invertedDocumentMap.put(filePath, idList);
			}

			// Add current document ID to list
			idList.add(documentId);
		}
		
		// Parse each document file and extract the titles of those documents searched-for.
		ArrayList<String> titles = new ArrayList<String>();
		for(Map.Entry<String, List<Integer>> entry: invertedDocumentMap.entrySet()) {
			PatentTitleLookup lookup = new PatentTitleLookup(new FileInputStream(entry.getKey()));
			Map<Integer, String> result = lookup.getTitles(entry.getValue());
			titles.addAll(result.values());
		}
		
		return titles;
	}
	
	private ArrayList<String> processLine(String line, String queryToken) throws FileNotFoundException, XMLStreamException{
		
		// Extract token from complete entry
		int index = line.indexOf(IndexWriter.TOKEN_POSTINGS_SEPARATOR);
		String token = line.substring(0, index);
		
		// If read token matches query token, get and return list of titles for the corresponding document IDs
		if(token.equals(queryToken)) {
			// Extract distinct document IDs from serialized posting lists
			String serializedPostingList = line.substring(index + 1);
			String[] serializedPostings = serializedPostingList.split(IndexWriter.POSTINGS_SEPARATOR);
			Integer[] documentIds = Arrays.stream(serializedPostings)
									.map(x -> Integer.parseInt(x.substring(1, x.indexOf(IndexWriter.POSTING_ENTRIES_SEPARATOR))))
									.distinct()
									.toArray(Integer[]::new);
			
			// Return titles for the corresponding document IDs
			return this.getDocumentTitles(documentIds);
		}
		
		return new ArrayList<String>();
	}
	
	private ArrayList<String> processCompressedLine(String line, String queryToken) throws FileNotFoundException, XMLStreamException{
		
		// Extract token from complete entry
		int index = line.indexOf(IndexWriter.TOKEN_POSTINGS_SEPARATOR);
		String token = line.substring(0, index);
		
		// If read token matches query token, get and return list of titles for the corresponding document IDs
		if(token.equals(queryToken)) {
			// Extract distinct document IDs from serialized posting lists
			String serializedPostingList = line.substring(index + 1);
			String[] serializedPostings = serializedPostingList.split(IndexWriter.POSTINGS_SEPARATOR);
			int lastDocId = 0;
			ArrayList<Integer> documentIds = new ArrayList<Integer>();
			
			for(int i = 0; i < serializedPostings.length; i++){
				Integer documentId = Integer.parseInt(serializedPostings[i].substring(1, serializedPostings[i].indexOf(",["))) + lastDocId;
				lastDocId = documentId;
				
				documentIds.add(documentId);
			}
			
			// Return titles for the corresponding document IDs
			return this.getDocumentTitles(documentIds.toArray(new Integer[documentIds.size()]));			 
		}
		
		return new ArrayList<String>();
	}
}
