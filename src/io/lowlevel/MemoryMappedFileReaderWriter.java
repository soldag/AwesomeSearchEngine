package io.lowlevel;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemoryMappedFileReaderWriter implements FileReader, FileWriter {

	/**
	 * Contains the actual file.
	 */
	private RandomAccessFile file;
	
	/**
	 * Contains the memory-mapped byte buffer of the file.
	 */
	private MappedByteBuffer buffer;
	

	/**
	 * Creates a new MemoryMappedFileReaderWriter instance for the given file.
	 * @param file
	 * @param mode
	 * @throws IOException 
	 */
	public MemoryMappedFileReaderWriter(File file, String mode) throws IOException {
		this.file = new RandomAccessFile(file, mode);
		
		FileChannel fileChannel = this.file.getChannel();
		FileChannel.MapMode mapMode = mode == "rw" ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.READ_ONLY;
		this.buffer = fileChannel.map(mapMode, 0, fileChannel.size());
	}
	
	
	@Override
	public void writeByte(byte value) throws IOException {
		this.buffer.put(value);
	}
	
	@Override
	public void write(byte[] bytes) throws IOException {
		this.buffer.put(bytes);
	}
	
	@Override
	public byte readByte() throws IOException {
		if(!this.buffer.hasRemaining()) {
			throw new EOFException();
		}
		
		return this.buffer.get();
	}
	
	@Override
	public int read(byte[] bytes) throws IOException {
		if(!this.buffer.hasRemaining()) {
			return -1;
		}
		
		int length = Math.min(bytes.length, this.buffer.remaining());
		this.buffer.get(bytes, 0, length);
		
		return length;
	}
	
	@Override
	public long getFilePointer() throws IOException {
		return this.buffer.position();
	}

	@Override
	public void seek(long pos) throws IOException {
		this.buffer.position((int)pos);
	}
	
	@Override
	public long length() throws IOException {
		return this.buffer.limit();
	}

	@Override
	public void close() throws IOException {
		this.file.close();
	}
}
