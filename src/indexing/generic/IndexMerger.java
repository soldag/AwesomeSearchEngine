package indexing.generic;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface IndexMerger {

	/**
	 *  Merges the given list of temporary index files and save result in destination file. 
	 * @param destinationIndexFile
	 * @param temporaryIndexFiles
	 * @param seekListFile
	 * @throws IOException
	 */
	public void merge(File destinationIndexFile, List<File> temporaryIndexFiles, File seekListFile) throws IOException;
}
