package querying;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
		// Identify boolean operators
		String operator = null;
		int operatorPosition = query.indexOf("AND");
		if(operatorPosition > 0) {
			operator = "AND";
		}
		if(operatorPosition < 0) {
			operatorPosition = query.indexOf("OR");
			operator = "OR";
		}
		if(operatorPosition < 0) {
			operatorPosition = query.indexOf("NOT");
			operator = "NOT";
		}
		if(operatorPosition >= 0) {
			String query1 = query.substring(0, operatorPosition - 1);
			String query2 = query.substring(operatorPosition + operator.length() + 1);
			
			ArrayList<String> result1 = this.runSingleQuery(query1);
			ArrayList<String> result2 = this.runSingleQuery(query2);
			

			if(operator == "NOT") {
				ArrayList<String> tempList = new ArrayList<String>();
				for(String result: result1) {
					if(!result2.contains(result)) {
						tempList.add(result);
					}
				}
				return tempList;
			}
			
			if(result1.size() > result2.size()) {
				ArrayList<String> tempList = result1;
				result1 = result2;
				result2 = tempList;
			}
			if(operator == "OR") {
				for(String result: result1) {
					if(!result2.contains(result)) {
						result2.add(result);
					}
				}
				return result2;
			}
			if(operator == "AND") {
				ArrayList<String> tempList = new ArrayList<String>();
				for(String result: result1) {
					if(result2.contains(result)) {
						tempList.add(result);
					}
				}
				return tempList;
			}
		}
		else {
			return this.runSingleQuery(query);
		}
		
		return null;
	}
	
	private ArrayList<String> runSingleQuery(String query) throws IOException, XMLStreamException {		
		// Tokenize(including stemming and stop-word-removal) query
		HashMap<Integer, List<Integer>> lastResult = new HashMap<Integer, List<Integer>>();
		String[] queryTokens = query.split(" ");
		for(String queryToken: queryTokens) {
			boolean prefixSearch =  queryToken.endsWith("*");
			HashMap<Integer, List<Integer>> currentResult;
			if(!prefixSearch) {
				List<Pair<String, Integer>> stemmedTokens = this.textProcessor.getTokens(queryToken);
				if(stemmedTokens.size() == 0) {
					continue;
				}
				queryToken = stemmedTokens.get(0).getValue0();
				currentResult = this.searchIndexWithoutPrefix(queryToken);
			}
			else {
				queryToken = queryToken.substring(0, queryToken.length() - 1);
				currentResult = this.searchIndexWithPrefix(queryToken);
			}
			
			if(lastResult.isEmpty()) {
				lastResult = currentResult;
			}
			else {
				HashMap<Integer, List<Integer>> tempResult = new HashMap<Integer, List<Integer>>();
				for(Map.Entry<Integer, List<Integer>> entry: lastResult.entrySet()) {
					int documentId = entry.getKey();
					if(currentResult.keySet().contains(documentId)) {
						for(int offset1: entry.getValue()) {
							for(int offset2: currentResult.get(documentId)) {
								if(offset2 == offset1 + 1) {
									if(tempResult.containsKey(documentId)) {
										tempResult.get(documentId).add(offset2);
									}
									else {
										List<Integer> offsets = new ArrayList<Integer>();
										offsets.add(offset2);
										tempResult.put(documentId, offsets);
									}
								}
							}
						}
					}
				}
				lastResult = tempResult;
			}
		}
		
		return this.getDocumentTitles(lastResult.keySet());
	}
	
	private HashMap<Integer, List<Integer>> searchIndexWithPrefix(String prefix) throws FileNotFoundException, IOException, XMLStreamException {
		Pair<Long, Long> indexOffsets = this.getIndexOffsets(prefix);
		Long startOffset = indexOffsets.getValue0();
		Long endOffset = indexOffsets.getValue1();
		
		List<String> seekListLines = new ArrayList<String>();
		if(this.compressed){			
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
						if(token.startsWith(prefix)) {
							seekListLines.add(line);
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
						if(token.startsWith(prefix)) {
							seekListLines.add(line);
						}
					}
				}
			}
		}

		HashMap<Integer, List<Integer>> documents = new HashMap<Integer, List<Integer>>();
		for(String seekListLine: seekListLines) {
			long offset = Long.parseLong(seekListLine.substring(seekListLine.indexOf(SeekListWriter.SEPARATOR) + 1));
			try(RandomAccessFile indexReader = new RandomAccessFile(this.indexFile, "r")) {
				indexReader.seek(offset);
				String line = indexReader.readLine();
				if(this.compressed) {
					for(Map.Entry<Integer, List<Integer>> entry: this.processCompressedLine(line, prefix, true).entrySet()) {
						if(documents.containsKey(entry.getKey())) {
							documents.get(entry.getKey()).addAll(entry.getValue());
						}
						else {
							documents.put(entry.getKey(), entry.getValue());
						}
					}
				}
				else {
					for(Map.Entry<Integer, List<Integer>> entry: this.processLine(line, prefix, true).entrySet()) {
						if(documents.containsKey(entry.getKey())) {
							documents.get(entry.getKey()).addAll(entry.getValue());
						}
						else {
							documents.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}
		}
		
		return documents;
	}
	
	private HashMap<Integer, List<Integer>> searchIndexWithoutPrefix(String queryToken) throws FileNotFoundException, IOException, XMLStreamException {
		Pair<Long, Long> indexOffsets = this.getIndexOffsets(queryToken);
		Long startOffset = indexOffsets.getValue0();
		Long endOffset = indexOffsets.getValue1();
		
		String seekListLine = null;
		if(this.compressed){			
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

		if(seekListLine != null) {
			long offset = Long.parseLong(seekListLine.substring(seekListLine.indexOf(SeekListWriter.SEPARATOR) + 1));
			try(RandomAccessFile indexReader = new RandomAccessFile(this.indexFile, "r")) {
				indexReader.seek(offset);
				String line = indexReader.readLine();
				if(this.compressed) {
					return this.processCompressedLine(line, queryToken, true);
				}
				else {
					return this.processLine(line, queryToken, true);
				}
			}
		}
		
		return new HashMap<Integer, List<Integer>>();
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
	private ArrayList<String> getDocumentTitles(Set<Integer> documentIds) throws FileNotFoundException, XMLStreamException {		
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
	
	private HashMap<Integer, List<Integer>> processLine(String line, String queryToken, boolean prefixSearch) throws FileNotFoundException, XMLStreamException{
		
		// Extract token from complete entry
		int index = line.indexOf(IndexWriter.TOKEN_POSTINGS_SEPARATOR);
		String token = line.substring(0, index);
		
		// If read token matches query token, get and return list of titles for the corresponding document IDs
		if((!prefixSearch && token.equals(queryToken)) || (prefixSearch && token.startsWith(queryToken))) {
			// Extract distinct document IDs from serialized posting lists
			String serializedPostingList = line.substring(index + 1);
			String[] serializedPostings = serializedPostingList.split(IndexWriter.POSTINGS_SEPARATOR);
			HashMap<Integer, List<Integer>> documents = new HashMap<Integer, List<Integer>>();
			for(String serializedPosting: serializedPostings) {
				int separatorIndex = serializedPosting.indexOf(IndexWriter.POSTING_ENTRIES_SEPARATOR);
				int documentId = Integer.parseInt(serializedPosting.substring(1, separatorIndex));
				int offset = Integer.parseInt(serializedPosting.substring(separatorIndex + 1, serializedPosting.length() - 1));
				if(documents.containsKey(documentId)) {
					documents.get(documentId).add(offset);
				}
				else {
					List<Integer> offsets = new ArrayList<Integer>();
					offsets.add(offset);
					documents.put(documentId, offsets);
				}
			}
			
			return documents;
		}
		
		return new HashMap<Integer, List<Integer>>();
	}
	
	private HashMap<Integer, List<Integer>> processCompressedLine(String line, String queryToken, boolean prefixSearch) throws FileNotFoundException, XMLStreamException{
		
		// Extract token from complete entry
		int index = line.indexOf(IndexWriter.TOKEN_POSTINGS_SEPARATOR);
		String token = line.substring(0, index);
		
		// If read token matches query token, get and return list of titles for the corresponding document IDs
		if((!prefixSearch && token.equals(queryToken)) || (prefixSearch && token.startsWith(queryToken))) {
			// Extract distinct document IDs from serialized posting lists
			String serializedPostingList = line.substring(index + 1);
			String[] serializedPostings = serializedPostingList.split(IndexWriter.POSTINGS_SEPARATOR);
			int lastDocId = 0;
			HashMap<Integer, List<Integer>> documents = new HashMap<Integer, List<Integer>>();
			for(int i = 0; i < serializedPostings.length; i++){
				int separatorPos = serializedPostings[i].indexOf(",[");
				Integer documentId = Integer.parseInt(serializedPostings[i].substring(1, separatorPos)) + lastDocId;
				lastDocId = documentId;

				String[] serializedOffsets = serializedPostings[i].substring(separatorPos + 2, serializedPostings[i].length() - 2).split(IndexWriter.POSTING_ENTRIES_SEPARATOR);
				List<Integer> offsets = new ArrayList<Integer>();
				int lastOffset = 0;
				for(String serializedOffset: serializedOffsets) {
					int offset = Integer.parseInt(serializedOffset) + lastOffset;
					lastOffset = offset;
					offsets.add(offset);
				}
				
				documents.put(documentId, offsets);
			}
			
			return documents;		 
		}
		
		return new HashMap<Integer, List<Integer>>();
	}
}
