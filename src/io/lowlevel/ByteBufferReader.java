package io.lowlevel;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferReader implements FileReader {
	
	/**
	 * Contains the memory byte buffer.
	 */
	private ByteBuffer buffer;
	
	
	/**
	 * Creates a new ByteBufferReader instance for the given byte array.
	 * @param buffer
	 */
	public ByteBufferReader(byte[] buffer) {
		this.buffer = ByteBuffer.wrap(buffer);
	}
	

	@Override
	public long getFilePointer() throws IOException {
		return buffer.position();
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
		this.buffer.clear();
	}

	@Override
	public int read(byte[] bytes) throws IOException {
		int length = bytes.length;
		if(bytes.length < this.buffer.remaining()) {
			length = this.buffer.remaining();
		}
		this.buffer.get(bytes, 0, length);
		
		return length;
	}

	@Override
	public byte readByte() throws IOException {
		return this.buffer.get();
	}

	@Override
	public byte[] readToEnd() throws IOException {
		return this.buffer.array();
	}
}
