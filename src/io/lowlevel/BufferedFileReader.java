package io.lowlevel;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferedFileReader implements FileReader {
	
	/**
	 * Contains the default length of the buffer.
	 */
	private static final int DEFAULT_BUFFER_LENGTH = 64000;

	/**
	 * Contains the corresponding file reader.
	 */
	private FileReader fileReader;
	
	/**
	 * Contains the reading buffer.
	 */
	private ByteBuffer buffer = null;
	
	/**
	 * Contains the length of the buffer.
	 */
	private int bufferLength;
	
	
	/**
	 * Creates a new BufferedFileReader instance.
	 * @param fileReader
	 */
	public BufferedFileReader(FileReader fileReader) {
		this(fileReader, DEFAULT_BUFFER_LENGTH);
	}
	
	/**
	 * Creates a new BufferedFileReader instance.
	 * @param fileReader
	 * @param bufferLength
	 */
	public BufferedFileReader(FileReader fileReader, int bufferLength) {
		this.fileReader = fileReader;
		this.bufferLength = bufferLength;
	}
	
	
	@Override
	public int read(byte[] bytes) throws IOException {
		// Check, if end of file has been reached
		if(this.getFilePointer() >= this.length()) {
			return -1;
		}
		
		// If buffer was not initialized yet, fill buffer
		if(this.buffer == null) {
			this.fillBuffer();
		}
		
		// Check, if requested bytes are in buffer or have to be read from disk
		if(bytes.length <= this.buffer.remaining()) {
			this.buffer.get(bytes);
			
			// Check, if buffer has been read completely and needs to be refilled
			if(this.buffer.remaining() == 0) {
				this.fillBuffer();
			}
		}
		else {
			// Read all bytes from buffer
			byte[] bufferBytes = new byte[this.buffer.remaining()];
			this.buffer.get(bufferBytes);
			
			// Read remaining bytes and new buffer from file
			int remainingBytes = bytes.length - bufferBytes.length;
			byte[] fileBytes = new byte[remainingBytes + this.bufferLength];
			int fileBytesLength = this.fileReader.read(fileBytes);
			
			// Copy bytes read from buffer into output array
			System.arraycopy(bufferBytes, 0, bytes, 0, bufferBytes.length);
			
			// Fill output array with bytes read from file and copy remaining ones into buffer
			if(fileBytesLength == -1) {
				return bufferBytes.length;
			}
			else if(fileBytesLength <= remainingBytes) {
				// End of file has been reached, so just fill output array
				System.arraycopy(fileBytes, 0, bytes, bufferBytes.length, fileBytesLength);
				
				return bufferBytes.length + fileBytesLength;
			}
			else {
				System.arraycopy(fileBytes, 0, bytes, bufferBytes.length, remainingBytes);
				
				int buffferLength = fileBytesLength - remainingBytes;
				byte[] newBuffer = new byte[buffferLength];
				System.arraycopy(fileBytes, remainingBytes, newBuffer, 0, buffferLength);
				this.buffer = ByteBuffer.wrap(newBuffer);
				this.buffer.limit(newBuffer.length);
			}
		}
		
		return bytes.length;
	}

	@Override
	public byte readByte() throws IOException {
		byte[] bytes = new byte[1];
		this.read(bytes);
		
		return bytes[0];
	}

	private void fillBuffer() throws IOException {
		byte[] fileBytes = new byte[this.bufferLength];
		int length = this.fileReader.read(fileBytes);
		
		if(length > 0) {
			this.buffer = ByteBuffer.wrap(fileBytes, 0, length);
			this.buffer.limit(length);
		}
	}

	
	@Override
	public long getFilePointer() throws IOException {
		return this.fileReader.getFilePointer() - this.buffer.remaining();
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
		long filePosition = this.fileReader.getFilePointer();
		if(pos >= filePosition - this.bufferLength && pos < filePosition) {
			// Skip to position in buffer
			long bufferPosition = pos - this.fileReader.getFilePointer() + this.buffer.capacity();
			this.buffer.position((int)bufferPosition);
		}
		else {
			// Skip to position in file and fill buffer
			this.fileReader.seek(pos);
			this.fillBuffer();
		}
	}

	@Override
	public long length() throws IOException {
		return this.fileReader.length();
	}

	@Override
	public void close() throws IOException {
		this.fileReader.close();		
		this.buffer = null;
	}
}
