package io.index;

import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

import io.lowlevel.FileWriter;

public class UncompressedIndexWriter implements IndexWriter, AutoCloseable {
	
	/**
	 * Contains the encoding used for storing strings.
	 */
	private static final String ENCODING = "UTF-8";

	/**
	 * Contains the corresponding file writer.
	 */
	private FileWriter fileWriter;
	
	/**
	 * Determines, whether a skipping area is active.
	 */
	private boolean skippingAreaActive = false;
	
	/**
	 * Contains a buffer for write operations in a skipping area.
	 */
	private ByteArrayOutputStream skippingAreaBuffer = new ByteArrayOutputStream();
	

	/**
	 * Creates a new UncompressedIndexWriter instance using the given FileWriter.
	 * @param fileWriter
	 */
	public UncompressedIndexWriter(FileWriter fileWriter) {
		this.fileWriter = fileWriter;
	}
	
	
	@Override
	public void write(byte[] bytes) throws IOException {
		if(this.skippingAreaActive) {
			this.skippingAreaBuffer.write(bytes);
		}
		else {
			this.fileWriter.write(bytes);
		}
	}
	
	public void writeByte(byte value) throws IOException {
		if(this.skippingAreaActive) {
			this.skippingAreaBuffer.write(value);
		}
		else {
			this.fileWriter.writeByte(value);
		}
	}

	@Override
	public void writeShort(short value) throws IOException {
		byte[] bytes = Shorts.toByteArray(value);
		this.write(bytes);
	}

	@Override
	public void writeInt(int value) throws IOException {
		byte[] bytes = Ints.toByteArray(value);
		this.write(bytes);
	}

	@Override
	public void writeLong(long value) throws IOException {
		byte[] bytes = Longs.toByteArray(value);
		this.write(bytes);
	}
	
	@Override
	public void writeString(String string) throws IOException {
		byte[] bytes = string.getBytes(ENCODING);
		this.writeInt(bytes.length);
		this.write(bytes);
	}
	

	@Override
	public void startSkippingArea() throws IOException {
		this.skippingAreaActive = true;
	}

	@Override
	public void endSkippingArea() throws IOException {
		if(!this.skippingAreaActive) {
			throw new IllegalStateException("A skipping area has to be started frist before ending it.");
		}
		
		// Write buffered bytes
		this.skippingAreaActive = false;
		this.flushSkippingArea();
		
		// Reset skipping area
		this.skippingAreaBuffer.reset();
	}
	
	private void flushSkippingArea() throws IOException {
		byte[] buffer = this.skippingAreaBuffer.toByteArray();
		this.writeInt(buffer.length);
		this.write(buffer);
	}
	
	@Override
	public boolean isCompressed() {
		return false;
	}
	
	@Override
	public IndexWriter uncompressed() {
		return this;
	}


	@Override
	public long getFilePointer() throws IOException {
		return this.fileWriter.getFilePointer();
	}
	
	public long length() throws IOException {
		return this.fileWriter.length();
	}

	@Override
	public void seek(long pos) throws IOException {
		this.fileWriter.seek(pos);
	}
	

	@Override
	public void close() throws IOException {
		if(this.skippingAreaActive) {
			this.flushSkippingArea();
		}
		
		this.fileWriter.close();
	}
}
