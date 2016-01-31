package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import io.index.CompressedIndexReader;
import io.index.CompressedIndexWriter;
import io.index.IndexReader;
import io.index.IndexWriter;
import io.index.UncompressedIndexReader;
import io.index.UncompressedIndexWriter;
import io.lowlevel.BufferedFileReader;
import io.lowlevel.BufferedFileWriter;
import io.lowlevel.ByteBufferReader;
import io.lowlevel.DirectFileReaderWriter;
import io.lowlevel.FileReader;
import io.lowlevel.FileWriter;
import io.lowlevel.MemoryMappedFileReaderWriter;

public class FileReaderWriterFactory {
	
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
	private static FileReaderWriterFactory instance;
	
	/**
	 * Returns the singleton instance of the factory.
	 * @return
	 */
	public static FileReaderWriterFactory getInstance() {
		if(instance == null) {
			instance = new FileReaderWriterFactory();
		}
		
		return instance;
	}
	
	
	/**
	 * Creates a new DirectIndexReader instance.
	 * @param file
	 * @param compress
	 * @return
	 * @throws FileNotFoundException
	 */
	public IndexReader getDirectIndexReader(File file, boolean compress) throws FileNotFoundException {
		FileReader fileReader = this.getDirectFileReader(file);
		return this.getIndexReader(fileReader, compress);
	}
	
	/**
	 * Creates a new BufferedIndexReader instance.
	 * @param file
	 * @param compress
	 * @return
	 * @throws IOException
	 */
	public IndexReader getBufferedIndexReader(File file, boolean compress) throws IOException {
		FileReader fileReader = this.getBufferedFileReader(file);
		return this.getIndexReader(fileReader, compress);
	}
	
	/**
	 * Creates a new MemoryMappedIndexReader instance.
	 * @param file
	 * @param compress
	 * @return
	 * @throws IOException
	 */
	public IndexReader getMemoryMappedIndexReader(File file, boolean compress) throws IOException {
		FileReader fileReader = this.getMemoryMappedFileReader(file);
		return this.getIndexReader(fileReader, compress);
	}
	
	/**
	 * Creates a new ByteBufferIndexReader instance.
	 * @param buffer
	 * @param compress
	 * @return
	 * @throws IOException
	 */
	public IndexReader getByteBufferIndexReader(byte[] buffer, boolean compress) throws IOException {
		FileReader fileReader = this.getByteBufferReader(buffer);
		return this.getIndexReader(fileReader, compress);
	}
	
	/**
	 * Creates a new IndexReader instance for the given FileReader instance.
	 * @param fileReader
	 * @param compress
	 * @return
	 */
	private IndexReader getIndexReader(FileReader fileReader, boolean compress) {
		if(compress) {
			return new CompressedIndexReader(fileReader);
		}
		else {
			return new UncompressedIndexReader(fileReader);
		}
	}

	
	/**
	 * Creates a new DirectIndexWriter instance.
	 * @param file
	 * @param compress
	 * @return
	 * @throws FileNotFoundException
	 */
	public IndexWriter getDirectIndexWriter(File file, boolean compress) throws FileNotFoundException {
		FileWriter fileWriter = this.getDirectFileWriter(file);
		return this.getIndexWriter(fileWriter, compress);
	}
	
	/**
	 * Creates a new BufferedIndexWriter instance.
	 * @param file
	 * @param compress
	 * @return
	 * @throws IOException
	 */
	public IndexWriter getBufferedIndexWriter(File file, boolean compress) throws IOException {
		FileWriter fileWriter = this.getBufferedFileWriter(file);
		return this.getIndexWriter(fileWriter, compress);
	}
	
	/**
	 * Creates a new MemoryMappedIndexWriter.
	 * @param file
	 * @param compress
	 * @return
	 * @throws IOException
	 */
	public IndexWriter getMemoryMappedIndexWriter(File file, boolean compress) throws IOException {
		FileWriter fileWriter = this.getMemoryMappedFileWriter(file);
		return this.getIndexWriter(fileWriter, compress);
	}
	
	/**
	 * Creates a new IndexWriter instance for the given FileWriter instance.
	 * @param fileWriter
	 * @param compress
	 * @return
	 */
	private IndexWriter getIndexWriter(FileWriter fileWriter, boolean compress) {
		if(compress) {
			return new CompressedIndexWriter(fileWriter);
		}
		else {
			return new UncompressedIndexWriter(fileWriter);
		}
	}	
	
	
	/**
	 * Creates a new DirectFileReader instance.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileReader getDirectFileReader(File file) throws FileNotFoundException {
		return new DirectFileReaderWriter(file, READ_MODE);
	}
	
	/**
	 * Creates a new DirectFileWriter instance.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileWriter getDirectFileWriter(File file) throws FileNotFoundException {
		return new DirectFileReaderWriter(file, WRITE_MODE);
	}
	
	/**
	 * Creates a new BufferedFileReader instance.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileReader getBufferedFileReader(File file) throws FileNotFoundException {
		return new BufferedFileReader(this.getDirectFileReader(file));
	}
	
	/**
	 * Creates a new BufferedFileWriter instance.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileWriter getBufferedFileWriter(File file) throws FileNotFoundException {
		return new BufferedFileWriter(this.getDirectFileWriter(file));
	}
	
	/**
	 * Creates a new MemoryMappedFileReader instance.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public FileReader getMemoryMappedFileReader(File file) throws IOException {
		return new MemoryMappedFileReaderWriter(file, READ_MODE);
	}
	
	/**
	 * Creates a new MemoryMappedFileWriter instance.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public FileWriter getMemoryMappedFileWriter(File file) throws IOException {
		return new MemoryMappedFileReaderWriter(file, WRITE_MODE);
	}
	
	/**
	 * Creates a new ByteBufferReader instance.
	 * @param buffer
	 * @return
	 * @throws IOException
	 */
	public FileReader getByteBufferReader(byte[] buffer) throws IOException {
		return new ByteBufferReader(buffer);
	}
}
