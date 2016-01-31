package io.index;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

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
	 * Contains the skipping area stack.
	 */
	private Deque<ByteArrayOutputStream> skippingAreaStack = new ArrayDeque<ByteArrayOutputStream>();
	

	/**
	 * Creates a new UncompressedIndexWriter instance using the given FileWriter.
	 * @param fileWriter
	 */
	public UncompressedIndexWriter(FileWriter fileWriter) {
		this.fileWriter = fileWriter;
	}
	
	
	@Override
	public void write(byte[] bytes) throws IOException {
		if(this.skippingAreaStack.isEmpty()) {
			this.fileWriter.write(bytes);
		}
		else {
			this.skippingAreaStack.peekFirst().write(bytes);
		}
	}
	
	public void writeByte(byte value) throws IOException {
		if(this.skippingAreaStack.isEmpty()) {
			this.fileWriter.writeByte(value);
		}
		else {
			this.skippingAreaStack.peekFirst().write(value);
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
	public void writeFloat(float value) throws IOException {
		int bits = Float.floatToIntBits(value);
		this.writeInt(bits);
	}


	@Override
	public void writeDouble(double value) throws IOException {
		long bits = Double.doubleToLongBits(value);
		this.writeLong(bits);
	}
	
	@Override
	public void writeString(String string) throws IOException {
		byte[] bytes = string.getBytes(ENCODING);
		this.writeInt(bytes.length);
		this.write(bytes);
	}
	

	@Override
	public void startSkippingArea() throws IOException {
		this.skippingAreaStack.push(new ByteArrayOutputStream());
	}

	@Override
	public void endSkippingArea() throws IOException {
		if(this.skippingAreaStack.isEmpty()) {
			throw new IllegalStateException("A skipping area has to be started first before ending it.");
		}
		
		this.flushSkippingArea();
	}
	
	private void flushSkippingArea() throws IOException {
		byte[] buffer = this.skippingAreaStack.removeFirst().toByteArray();
		this.writeInt(buffer.length);
		this.write(buffer);
	}
	
	@Override
	public boolean isCompressed() {
		return false;
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
		if(!this.skippingAreaStack.isEmpty()) {
			this.flushSkippingArea();
		}
		
		this.fileWriter.close();
	}
}
