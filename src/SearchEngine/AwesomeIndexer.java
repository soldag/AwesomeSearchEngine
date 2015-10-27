package SearchEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class AwesomeIndexer {
	
	// XPaths constants for XML elements containing the document ID and abstract of a patent.
	private static final String DOCUMENT_ID_XPATH = "my-root/us-patent-grant/us-bibliographic-data-grant/publication-reference/document-id/doc-number";
	private static final String ABSTRACT_XPATH = "my-root/us-patent-grant/abstract";
	
	// Determines, how many tokens the memory index can contain, before it has to be written to file.
	private static final int MEMORY_INDEX_LIMIT = 500;
	
	// Determines, how many often a token should be written to seek list (every n-th token).
	private static final int SEEK_LIST_TOKEN_LIMIT = 20;
	
	// Constants for directory and file names of temporary indexes.
	private static final String TEMP_INDEX_DIRECTORY = "tmp";
	private static final String TEMP_INDEX_PREFIX = "index_%d";
	
	// Separators for the serialization of index entries and posting lists.
	public static final String KEY_VALUE_SEPARATOR = "|";
	public static final String POSTINGS_SEPARATOR = ";";
	public static final String POSTING_ENTRY_SEPARATOR = ",";

	// Contain instances of necessary services
	private AwesomeTextProcessor textProcessor;
	private AwesomeIndexEntryComparator indexEntryComparator;
	
	// Contain various index files.
	private File indexFile;
	private File seekListFile;
	private File documentMapFile;
	
	// Contains path of the directory used for temporary indexes.
	private Path tempIndexDirectory;
	
	// Contains the currently constructed part of the index in memory
	private HashMap<String, List<Pair<String, Integer>>> invertedIndex = new HashMap<String, List<Pair<String, Integer>>>();
	
	// Contains the number of token stored in the inverted index in memory
	private int memoryTokenCount = 0;
	
	
	public AwesomeIndexer(AwesomeTextProcessor textProcessor, Path indexDirectoryPath, File indexFile, File seekListFile, File documentMapFile) {
		this.textProcessor = textProcessor;
		
		this.indexFile = indexFile;
		this.seekListFile = seekListFile;
		this.documentMapFile = documentMapFile;
		this.tempIndexDirectory = indexDirectoryPath.resolve(TEMP_INDEX_DIRECTORY);
		
		this.indexEntryComparator = new AwesomeIndexEntryComparator(KEY_VALUE_SEPARATOR);
	}
	
	
	// Parses a document and indexes the abstract.
	public void parseDocument(String documentPath) throws XMLStreamException, IOException {
		// Clear existing, temporary indexes and create temporary index directory
		this.clearTemporaryIndexes();
		this.tempIndexDirectory.toFile().mkdirs();
		
		// Open document map file for construction
		try (BufferedWriter documentMapWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.documentMapFile), "UTF-8"))) {	
			XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
			XMLStreamReader xmlParser = xmlFactory.createXMLStreamReader(new FileInputStream(documentPath));
			
			String currentXPath = "";
			String currentDocumentId = "";
			StringBuilder abstractBuilder = new StringBuilder();
			while (xmlParser.hasNext())
			{
				int eventType = xmlParser.getEventType();
				if(eventType == XMLStreamReader.START_ELEMENT) {
					// Update current path
					if(currentXPath != null && !currentXPath.isEmpty()) 
			    	{
			        	currentXPath += "/";
			    	}
					currentXPath += xmlParser.getLocalName();
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
			    	
			    	// Check, if abstract element has been closed, so that the whole text was read and can be indexed
			    	if(currentXPath.equals(ABSTRACT_XPATH)) {
			    		// Tokenize(including stemming and stop-word-removal) abstract and add single tokens to index
			    		for(Pair<String, Integer> token: this.textProcessor.getTokens(abstractBuilder.toString())) {
			    			this.add(currentDocumentId, token.getValue0(), token.getValue1());
			    			this.memoryTokenCount++;
			    			
			    			// If size of the memory index is too high, write index to new temporary file and clear index in memory
			    			if(this.memoryTokenCount >= MEMORY_INDEX_LIMIT) {
			    				this.writeToFile();
			    				this.memoryTokenCount = 0;
			    				this.invertedIndex.clear();
			    			}
			    		}
			    		
			    		// Clear existing abstract builder in order to build next abstract
			    		abstractBuilder = new StringBuilder();
			    	}
				}
				else if (eventType == XMLStreamReader.CHARACTERS) {
					if(currentXPath.equals(DOCUMENT_ID_XPATH)) {
						// Extract current document ID
						currentDocumentId = xmlParser.getText();
						
						// Write mapping of document ID and file name to document map
						documentMapWriter.write(currentDocumentId + KEY_VALUE_SEPARATOR + documentPath);
						documentMapWriter.newLine();
			    	}
			    	else if(currentXPath.startsWith(ABSTRACT_XPATH))
			    	{
			    		// Append (next) part of the abstract text.
			    		abstractBuilder.append(xmlParser.getText().trim());
			    	}
				}
				
				// Read next element
				xmlParser.next();
			}
			
			xmlParser.close();
		}
		
		// Write remaining index entries to file
		if(this.invertedIndex.size() > 0) {
			this.writeToFile();
		}
		
		// Merge temporary index files
		this.merge();
		
		// Reset state of indexer
		this.clearTemporaryIndexes();
		this.reset();
	}
	
	
	// Adds new token to index.
	private void add(String documentId, String token, int offset) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		// Get posting list
		List<Pair<String, Integer>> postingList;
		if(this.invertedIndex.containsKey(token)) {
			// Get posting list for given token
			postingList = this.invertedIndex.get(token);
		}
		else {
			// Create new, empty posting list for given token
			postingList = new ArrayList<Pair<String, Integer>>();
			this.invertedIndex.put(token, postingList);
		}
		
		// Add current document ID and offset to posting list
		postingList.add(new Pair<String, Integer>(documentId, offset));
	}
	
	
	// Write index part from memory to temporary file.
	private void writeToFile() throws UnsupportedEncodingException, FileNotFoundException, IOException {
		// Sort tokens
		List<String> tokens = new ArrayList<String>(this.invertedIndex.keySet());
		Collections.sort(tokens);
		
		// Write token and posting list to file line by line (one line per token)
		File indexFile = File.createTempFile(TEMP_INDEX_PREFIX, "", this.tempIndexDirectory.toFile());
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFile, false), "UTF-8"))) {
			for(String token: tokens) {
				// Serialize posting list
				String serializedPostingList = this.invertedIndex.get(token).stream()
												.map(x -> String.format("[%s%s%s]", x.getValue0(), POSTING_ENTRY_SEPARATOR, x.getValue1()))
												.collect(Collectors.joining(POSTINGS_SEPARATOR));
				
				// Write line to file
				writer.write(token + KEY_VALUE_SEPARATOR + serializedPostingList);
				writer.newLine();
			}
		}
	}
	
	
	// Merge multiple temporary index files to a single, complete one.
	private void merge() throws UnsupportedEncodingException, IOException {
		// Open index files
		File[] tempIndexFiles = this.tempIndexDirectory.toFile().listFiles();
		
		if(tempIndexFiles.length == 1){
			// No need to merge, so just copy temporary file
			Files.move(tempIndexFiles[0].toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		else {
			// Create scanners for temporary indexes
			List<Scanner> tempIndexScanners = new ArrayList<Scanner>(tempIndexFiles.length);
			for(File tempIndexFile: tempIndexFiles) {
				tempIndexScanners.add(new Scanner(new FileInputStream(tempIndexFile), "UTF-8"));
			}

			// Write resulting, overall index file
			try (
				RandomAccessFile indexWriter = new RandomAccessFile(this.indexFile, "rw");
				BufferedWriter seekListWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.seekListFile), "UTF-8"))
			) {
				// Read first line of each temporary index file
				List<String> lines = tempIndexScanners.stream().map(x -> x.nextLine()).collect(Collectors.toList());
				
				int tokenCount = 0;
				String lastToken = null;
				while(!tempIndexScanners.isEmpty()) {
					// Determine minimum line of read lines
					String minLine = lines.stream().min(this.indexEntryComparator).get();
					
					int scannerIndex = lines.indexOf(minLine);
					int separatorIndex = minLine.indexOf(KEY_VALUE_SEPARATOR);
					String token =  minLine.substring(0, separatorIndex);
					
					if(token.equals(lastToken)) {
						// Token already exists in index file, so just concatenate posting lists
						String postings = POSTINGS_SEPARATOR + minLine.substring(separatorIndex + 1);
						indexWriter.write(postings.getBytes());
					}
					else {
						// Write a new line to file (unless its the first ever token), since the token does not exists in the index file so far
						if(lastToken != null) {
							indexWriter.write(System.getProperty("line.separator").getBytes());
						}

						// Reset number of tokens, if limit has been reached
						if(tokenCount == SEEK_LIST_TOKEN_LIMIT) {
							tokenCount = 0;
						}
						else if(tokenCount == 0) {
							// Write seek list entry for the current token
							long offset = indexWriter.getFilePointer();
							seekListWriter.write(token + KEY_VALUE_SEPARATOR + offset);
							seekListWriter.newLine();
						}
						tokenCount++;
						
						// Write token and posting list to index file
						indexWriter.write(minLine.getBytes());
					}
					
					// Update last token
					lastToken = token;

					// Read new line from the file, from which the just processed line was read.
					Scanner currentScanner = tempIndexScanners.get(scannerIndex);
					if(currentScanner.hasNext()) {
						lines.set(scannerIndex, currentScanner.nextLine());
					}
					else {
						// If EOF was reached, remove file from processing queue
						currentScanner.close();
						tempIndexScanners.remove(scannerIndex);
						lines.remove(scannerIndex);
					}
				}
			}
		}
	}
	
	
	// Reset indexer for reusage.
	public void reset() {
		this.memoryTokenCount = 0;
		this.invertedIndex.clear();
	}
	
	
	// Clear temporary files.
	public void clearTemporaryIndexes() throws IOException {
		FileUtils.deleteDirectory(this.tempIndexDirectory.toFile());
	}
}
