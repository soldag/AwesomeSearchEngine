package io.lowlevel;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferedFileWriter implements FileWriter {
	
	/**
	 * Contains the default length of the buffer.
	 */
	private static final int DEFAULT_BUFFER_SIZE = 64000;

	/**
	 * Contains the corresponding file writer.
	 */
	private FileWriter fileWriter;
	
	/**
	 * Contains the writing buffer.
	 */
	private ByteBuffer buffer;
	
	/**
	 * Contains the offset in the buffer, to which data has bee written to the buffer.
	 */
	private int bufferFillLength = 0;
	
	/**
	 * Creates a new BufferedFileWriter instance.
	 * @param fileWriter
	 */
	public BufferedFileWriter(FileWriter fileWriter) {
		this(fileWriter, DEFAULT_BUFFER_SIZE);
	}
	
	/**
	 * Creates a new BufferedFileWriter instance.
	 * @param fileWriter
	 * @param bufferSize
	 */
	public BufferedFileWriter(FileWriter fileWriter, int bufferSize) {
		this.fileWriter = fileWriter;
		this.buffer = ByteBuffer.allocate(bufferSize);
	}
	
	
	@Override
	public void write(byte[] bytes) throws IOException {
		// Check, if bytes fit into the buffer
		int remainingBytes = this.buffer.remaining();
		if(bytes.length <= remainingBytes) {
			this.buffer.put(bytes);
			
			// Update fill length
			if(this.buffer.position() > this.bufferFillLength) {
				this.bufferFillLength = this.buffer.position();
			}
			
			// If buffer is now full, flush to disk
			if(!buffer.hasRemaining()) {
				this.flush();
			}
		}
		else {
			// Write buffer and given bytes to disk
			this.flush();
			this.fileWriter.write(bytes);
		}
	}
	
	@Override
	public void writeByte(byte value) throws IOException {
		byte[] bytes = new byte[] { value };
		this.write(bytes);
	}
	
	private void flush() throws IOException {
		byte[] bytes = new byte[this.bufferFillLength];
		this.buffer.get(bytes);
		this.fileWriter.write(bytes);
		this.buffer.clear();
		this.bufferFillLength = 0;
	}
	

	@Override
	public long getFilePointer() throws IOException {
		return this.fileWriter.getFilePointer() + this.buffer.position();
	}

	@Override
	public long length() throws IOException {
		return this.fileWriter.getFilePointer();
	}

	@Override
	public void seek(long pos) throws IOException {
		// Check, if position is valid
		if(pos < 0) {
			throw new IOException("pos must be 0 or greater.");
		}
		else if (pos >= this.length()) {
			throw new EOFException();
		}
		
		// Check, if position is currently loaded in the buffer 
		long filePosition = this.fileWriter.getFilePointer();
		if(pos >= filePosition) {
			long bufferPosition = pos - filePosition;
			this.buffer.position((int)bufferPosition);
		}
		else {
			this.flush();
			this.fileWriter.seek(pos);
		}
	}
	
	@Override
	public void close() throws IOException {
		this.flush();
		this.fileWriter.close();
		
		this.buffer = null;
	}
}
