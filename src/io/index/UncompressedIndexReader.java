package io.index;

import java.io.IOException;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

import io.lowlevel.FileReader;

public class UncompressedIndexReader implements IndexReader, AutoCloseable {
	
	/**
	 * Contains the encoding used for storing strings.
	 */
	private static final String ENCODING = "UTF-8";
	
	/**
	 * Contains the corresponding file reader.
	 */
	private FileReader fileReader;
	
	
	/**
	 * Creates a new UncompressedIndexReader instance.
	 * @param fileReader
	 */
	public UncompressedIndexReader(FileReader fileReader) {
		this.fileReader = fileReader;
	}
	

	@Override
	public int read(byte[] bytes) throws IOException {
		return this.fileReader.read(bytes);
	}

	@Override
	public byte readByte() throws IOException {
		return this.fileReader.readByte();
	}

	@Override
	public short readShort() throws IOException {
		byte[] bytes = new byte[Short.BYTES];
		this.read(bytes);
		return Shorts.fromByteArray(bytes);
	}

	@Override
	public int readInt() throws IOException {
		byte[] bytes = new byte[Integer.BYTES];
		this.read(bytes);
		return Ints.fromByteArray(bytes);
	}

	@Override
	public long readLong() throws IOException {
		byte[] bytes = new byte[Long.BYTES];
		this.read(bytes);
		return Longs.fromByteArray(bytes);
	}

	@Override
	public String readString() throws IOException {
		int length = this.readInt();
		byte[] bytes = new byte[length];
		this.read(bytes);
		
		return new String(bytes, ENCODING);
	}
	

	@Override
	public int getSkippingAreaLenght() throws IOException {
		return this.readInt();
	}

	@Override
	public void skipSkippingArea() throws IOException {
		int length = this.getSkippingAreaLenght();
		this.seek(this.getFilePointer() +  length);
	}

	
	@Override
	public boolean isCompressed() {
		return false;
	}

	@Override
	public long getFilePointer() throws IOException {
		return this.fileReader.getFilePointer();
	}

	@Override
	public long length() throws IOException {
		return this.fileReader.length();
	}

	@Override
	public void seek(long pos) throws IOException {
		this.fileReader.seek(pos);
	}
	

	@Override
	public void close() throws IOException {
		this.fileReader.close();
	}
}
