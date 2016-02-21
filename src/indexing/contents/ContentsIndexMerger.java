package indexing.contents;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import io.FileReaderWriterFactory;
import io.index.IndexReader;
import io.index.IndexWriter;

public class ContentsIndexMerger {
	
	/**
	 * Determines, whether the index files are compressed or not.
	 */
	private boolean isCompressed;
	

	/**
	 * Creates a new ContentsIndexMerger instance.
	 * @param isCompressed
	 */
	public ContentsIndexMerger(boolean isCompressed) {
		this.isCompressed = isCompressed;
	}
	
	
	public void merge(File destinationIndexFile, List<File> temporaryIndexFiles, File seekListFile) throws IOException {
		// Initialize seeklist
		ContentsIndexSeekList seekList = new ContentsIndexSeekList();
		
		// Create destination index file
		try (IndexWriter destinationFileWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(destinationIndexFile, this.isCompressed)) {
			// Open temporary index files
			List<Integer> firstDocumnentIds = new ArrayList<Integer>(temporaryIndexFiles.size());
			List<IndexReader> sourceFiles = new ArrayList<IndexReader>(temporaryIndexFiles.size());
			int totalSize = 0;
			for(File temporaryIndexFile: temporaryIndexFiles) {
				IndexReader tempFile = FileReaderWriterFactory.getInstance().getBufferedIndexReader(temporaryIndexFile, this.isCompressed);
				sourceFiles.add(tempFile);
				totalSize += tempFile.readInt();
				firstDocumnentIds.add(tempFile.readInt());
			}
			
			// Write total documents count
			destinationFileWriter.writeInt(totalSize);

			while(firstDocumnentIds.size() > 0) {
				// Determine next document id index
				int nextDocumentIdIndex = IntStream.range(0, firstDocumnentIds.size())	
										.boxed()
										.min(Comparator.comparing(x -> firstDocumnentIds.get(x)))
										.get();
				IndexReader currentFile = sourceFiles.get(nextDocumentIdIndex);
				
				// Get document id and corresponding content
				int documentId = firstDocumnentIds.get(nextDocumentIdIndex);
				byte[] contentsBytes = currentFile.getSkippingAreaReader().readToEnd();
				
				// Write document id and content
				destinationFileWriter.writeInt(documentId);
				destinationFileWriter.startSkippingArea();
				destinationFileWriter.write(contentsBytes);
				destinationFileWriter.endSkippingArea();
				
				// Refresh tokens and source files list
				if(currentFile.getFilePointer() < currentFile.length()) {
					firstDocumnentIds.set(nextDocumentIdIndex, sourceFiles.get(nextDocumentIdIndex).readInt());
				}
				else {
					currentFile.close();
					sourceFiles.remove(nextDocumentIdIndex);
					firstDocumnentIds.remove(nextDocumentIdIndex);
				}
			}
		}
		
		// Write seek list to file
		try(IndexWriter seekListWriter = FileReaderWriterFactory.getInstance().getDirectIndexWriter(seekListFile, this.isCompressed)) {
			seekList.save(seekListWriter);
		}
	}
}
