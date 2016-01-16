package io.lowlevel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DirectFileReaderWriter implements FileReader, FileWriter {

	/**
	 * Contains the actual file.
	 */
	private final RandomAccessFile file;
	

	/**
	 * Creates a new DirectFileReaderWriter instance for the given file.
	 * @param file
	 * @throws FileNotFoundException
	 */
	public DirectFileReaderWriter(File file, String mode) throws FileNotFoundException {
		this.file = new RandomAccessFile(file, mode);
	}
	

	@Override
	public int read(byte[] bytes) throws IOException {
		return this.file.read(bytes);
	}

	@Override
	public byte readByte() throws IOException {
		return this.file.readByte();
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		this.file.write(bytes);
	}

	@Override
	public void writeByte(byte value) throws IOException {
		this.file.write(value);
	}

	@Override
	public void seek(long pos) throws IOException {
		this.file.seek(pos);
	}

	@Override
	public long getFilePointer() throws IOException {
		return this.file.getFilePointer();
	}

	@Override
	public long length() throws IOException {
		return this.file.length();
	}

	@Override
	public void close() throws IOException {
		this.file.close();
	}

}
