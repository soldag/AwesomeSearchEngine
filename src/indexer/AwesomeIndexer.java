package indexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.javatuples.Pair;

import parsers.PatentAbstractParser;
import textprocessing.AwesomeTextProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AwesomeIndexer {
	
	// Determines the limit of free bytes in memory. If this limit is exceeded, memory index has to be written to file.
	private static final int FREE_MEMORY_LIMIT = 512000000;
	
	// Constants for directory and file names of temporary indexes.
	private static final String TEMP_INDEX_DIRECTORY = "tmp";
	private static final String TEMP_INDEX_PREFIX = "index_%d";

	// Contain instances of necessary services
	private AwesomeTextProcessor textProcessor;
	private AwesomeIndexEntryComparator indexEntryComparator;
	
	// Contain various index files.
	private File indexFile;
	private File seekListFile;
	private File secondarySeekListFile;
	private File documentMapFile;
	
	// Contains path of the directory used for temporary indexes.
	private Path tempIndexDirectory;
	
	// Contains all created temporary index files
	private List<File> temporaryIndexFiles = new ArrayList<File>();
	
	// Contains the currently constructed part of the index in memory
	private HashMap<String, List<Pair<Integer, Long>>> invertedIndex = new HashMap<String, List<Pair<Integer, Long>>>();
	
	private boolean compressed;
	
	public AwesomeIndexer(AwesomeTextProcessor textProcessor, Path indexDirectoryPath, File indexFile, File seekListFile, File secondarySeekListFile, File documentMapFile, boolean compressed) {
		this.textProcessor = textProcessor;
		
		this.indexFile = indexFile;
		this.seekListFile = seekListFile;
		this.secondarySeekListFile = secondarySeekListFile;
		this.documentMapFile = documentMapFile;
		this.tempIndexDirectory = indexDirectoryPath.resolve(TEMP_INDEX_DIRECTORY);
		
		this.indexEntryComparator = new AwesomeIndexEntryComparator(IndexWriter.TOKEN_POSTINGS_SEPARATOR);
		this.compressed = compressed;
	}
	
	
	// Parses a document and indexes the abstract.
	public void indexDocument(String documentPath) throws XMLStreamException, IOException {
		// Clear existing indexes and create temporary index directory
		this.clearTemporaryIndexes();		
		this.deleteIndexFiles();
		this.tempIndexDirectory.toFile().mkdirs();
		
		// Parse and index document
		this.parseDocument(documentPath);
		
		// Determine, whether there are multiple temporary index files that need to be merged
		boolean needToMerge = this.temporaryIndexFiles.size() > 1;
		
		// Write remaining memory index entries to file
		if(this.invertedIndex.size() > 0) {
			this.writeToFile(!needToMerge);
		}
		
		// Merge temporary index files, if necessary
		if(needToMerge) {
			this.merge();
		}
		
		// Reset state of indexer
		this.clearTemporaryIndexes();
		this.invertedIndex.clear();
	}
	
	
	public void parseDocument(String documentPath) throws XMLStreamException, IOException {
		try (DocumentMapWriter documentMapWriter = new DocumentMapWriter(this.documentMapFile)) {	
			PatentAbstractParser patentParser = new PatentAbstractParser(new FileInputStream(documentPath));
			for(Pair<Integer, String> idAbstractTuple: patentParser) {
				// Unpack tuple
				Integer documentId = idAbstractTuple.getValue0();
				String abstractText = idAbstractTuple.getValue1();
				
				// Add entry to document map
				documentMapWriter.write(documentId, documentPath);
				
				// Tokenize(including stemming and stop-word-removal) abstract and add single tokens to index
	    		for(Pair<String, Integer> token: this.textProcessor.getTokens(abstractText)) {
	    			this.add(documentId, token.getValue0(), token.getValue1());
	    			
	    			// If size of the memory index is too high, write index to new temporary file and clear index in memory
	    			if(this.getFreeMemory() < FREE_MEMORY_LIMIT) {
	    				this.writeToFile(false);
	    				this.invertedIndex.clear();
	    				
	    				// Run garbage collector
	    				System.gc();
	    				System.runFinalization();
	    			}
	    		}
			}
			
			patentParser.close();
		}
	}
	
	
	// Returns number of free memory byte.
	private long getFreeMemory() {
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		return Runtime.getRuntime().maxMemory() - usedMemory;
	}
	
	
	// Adds new token to index.
	private void add(Integer documentId, String token, long offset) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		// Get posting list
		List<Pair<Integer, Long>> postingList;
		if(this.invertedIndex.containsKey(token)) {
			// Get posting list for given token
			postingList = this.invertedIndex.get(token);
		}
		else {
			// Create new, empty posting list for given token
			postingList = new ArrayList<Pair<Integer, Long>>();
			this.invertedIndex.put(token, postingList);
		}
		
		// Add current document ID and offset to posting list
		postingList.add(new Pair<Integer, Long>(documentId, offset));
	}
	
	
	// Write index part from memory to temporary file.
	private void writeToFile(boolean writeSeekList) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		// Sort tokens
		List<String> tokens = new ArrayList<String>(this.invertedIndex.keySet());
		Collections.sort(tokens);
		
		// Write token and posting list to file line by line (one line per token)
		File indexFile = File.createTempFile(TEMP_INDEX_PREFIX, "", this.tempIndexDirectory.toFile());
		IndexWriter indexWriter;
		if(writeSeekList) {
			indexWriter = new IndexWriter(this.indexFile, this.seekListFile, this.secondarySeekListFile, this.compressed);
		}
		else {
			indexWriter = new IndexWriter(this.indexFile, this.compressed);
		}
		for(String token: tokens) {
			indexWriter.write(token, this.invertedIndex.get(token));
		}
		indexWriter.close();
		this.temporaryIndexFiles.add(indexFile);
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
				SeekListWriter seekListWriter = new SeekListWriter(this.seekListFile, this.secondarySeekListFile);
			) {
				// Read first line of each temporary index file
				List<String> lines = tempIndexScanners.stream().map(x -> x.nextLine()).collect(Collectors.toList());
				
				String lastToken = null;
				while(!tempIndexScanners.isEmpty()) {
					// Determine minimum line of read lines
					String minLine = lines.stream().min(this.indexEntryComparator).get();
					
					int scannerIndex = lines.indexOf(minLine);
					int separatorIndex = minLine.indexOf(IndexWriter.TOKEN_POSTINGS_SEPARATOR);
					String token =  minLine.substring(0, separatorIndex);
					
					if(token.equals(lastToken)) {
						// Token already exists in index file, so just concatenate posting lists
						String postings = IndexWriter.POSTINGS_SEPARATOR + minLine.substring(separatorIndex + 1);
						indexWriter.write(postings.getBytes());
					}
					else {
						// Write a new line to file (unless its the first ever token), since the token does not exists in the index file so far
						if(lastToken != null) {
							indexWriter.write(System.getProperty("line.separator").getBytes());
						}

						// Write seek list entry for the current token
						seekListWriter.write(token, indexWriter.getFilePointer());
						
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
	
	
	// Clear temporary files.
	public void clearTemporaryIndexes() throws IOException {
		for(File indexFile: this.temporaryIndexFiles) {
			if(indexFile.exists()) {
				indexFile.delete();
			}
		}
		if(this.tempIndexDirectory.toFile().exists()) {
			this.tempIndexDirectory.toFile().delete();
		}
		this.temporaryIndexFiles.clear();
	}
	
	public void deleteIndexFiles() {
		if(this.indexFile.exists()) {
			this.indexFile.delete();
		}
		if(this.seekListFile.exists()) {
			this.seekListFile.delete();
		}if(this.secondarySeekListFile.exists()) {
			this.secondarySeekListFile.delete();
		}if(this.documentMapFile.exists()) {
			this.documentMapFile.delete();
		}
	}
}
