package indexing.documentmap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import indexing.generic.IndexMerger;
import io.FileReaderWriterFactory;
import io.index.IndexReader;
import io.index.IndexWriter;
import utilities.MapValueComparator;

public class DocumentMapMerger implements IndexMerger {
	
	/**
	 * Contains the length of the buffer.
	 */
	private int BUFFER_LENGTH = 6400;
	
	/**
	 * Determines, whether the index files are compressed or not.
	 */
	private boolean isCompressed;
	

	/**
	 * Creates a new DocumentMapMerger instance.
	 * @param isCompressed
	 */
	public DocumentMapMerger(boolean isCompressed) {
		this.isCompressed = isCompressed;
	}
	
	
	@Override
	public void merge(File destinationIndexFile, List<File> temporaryIndexFiles, File seekListFile) throws IOException {
		// Initialize seeklist
		DocumentMapSeekList seekList = new DocumentMapSeekList();
		
		// Create destination index file
		try (IndexWriter destinationFile = FileReaderWriterFactory.getInstance().getDirectIndexWriter(destinationIndexFile, this.isCompressed)) {
			// Open temporary index files
			Map<Integer, Integer> firstDocumentIds = new HashMap<Integer, Integer>(temporaryIndexFiles.size());
			List<IndexReader> sourceFiles = new ArrayList<IndexReader>(temporaryIndexFiles.size());
			long totalDocumentsCount = 0;
			for(int i = 0; i < temporaryIndexFiles.size(); i++) {
				IndexReader tempFile = FileReaderWriterFactory.getInstance().getBufferedIndexReader(temporaryIndexFiles.get(i), this.isCompressed);
				sourceFiles.add(tempFile);
				totalDocumentsCount += tempFile.readInt();
				firstDocumentIds.put(i, tempFile.readInt());
			}
			
			// Write total size
			destinationFile.writeLong(totalDocumentsCount);
			
			// Determine merge order
			int[] fileOrder = firstDocumentIds.entrySet().stream()
										.sorted(MapValueComparator.natural())
										.mapToInt(entry -> entry.getValue())
										.toArray();
			for(int fileIndex: fileOrder) {
				IndexReader currentFileReader = sourceFiles.get(fileIndex);
				
				// Write read document id
				destinationFile.writeInt(firstDocumentIds.get(fileIndex));
				
				// Copy remaining bytes
				byte[] buffer = new byte[BUFFER_LENGTH];
				while(currentFileReader.read(buffer) >= 0) {
					destinationFile.write(buffer);
				}
				
				currentFileReader.close();
			}
		}
		
		// Write seek list to file
		try(IndexWriter seekListWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(seekListFile, this.isCompressed)) {
			seekList.save(seekListWriter);
		}
	}
}
