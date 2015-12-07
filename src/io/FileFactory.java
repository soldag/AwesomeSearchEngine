package io;

import java.io.File;
import java.io.FileNotFoundException;

public class FileFactory {
	
	/**
	 * Contains the file mode for read access.
	 */
	private static final String READ_MODE = "r";
	
	/**
	 * Contains the file mode for read/write access.
	 */
	private static final String WRITE_MODE = "rw";
	
	/**
	 * Contains the singleton instance of the factory.
	 */
	private static FileFactory instance;
	
	/**
	 * Returns the singleton instance of the factory.
	 * @return
	 */
	public static FileFactory getInstance() {
		if(instance == null) {
			instance = new FileFactory();
		}
		
		return instance;
	}

	
	/**
	 * Creates a new (uncompressed) FileReader instance for the given file.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileReader getReader(File file) throws FileNotFoundException {
		return this.getReader(file, false);
	}
	
	/**
	 * Creates a new FileReader instance for the given file.
	 * @param file
	 * @param compressed
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileReader getReader(File file, boolean compressed) throws FileNotFoundException {
		return (FileReader)this.getFileOperator(file, compressed, READ_MODE);
	}
	
	/**
	 * Creates a new (uncompressed) FileWriter instance for the given file.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileWriter getWriter(File file) throws FileNotFoundException {
		return this.getWriter(file, false);
	}
	
	/**
	 * Creates a new FileWriter instance for the given file.
	 * @param file
	 * @param compressed
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileWriter getWriter(File file, boolean compressed) throws FileNotFoundException {
		return (FileWriter)this.getFileOperator(file, compressed, WRITE_MODE);
	}
	
	/**
	 * Creates a new FileOperator instance for the given file based on compressed and mode argument.
	 * @param file
	 * @param compressed
	 * @param mode
	 * @return
	 * @throws FileNotFoundException
	 */
	private FileOperator getFileOperator(File file, boolean compressed, String mode) throws FileNotFoundException {
		if(compressed) {
			return new CompressedIndexFile(file, mode);
		}
		else {
			return new IndexFile(file, mode);
		}
	}
}
