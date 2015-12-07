package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

public class IndexFile implements FileReader, FileWriter, AutoCloseable {
	
	/**
	 * Contains the encoding used for storing strings.
	 */
	private static final String ENCODING = "UTF-8";

	/**
	 * Contains the actual file.
	 */
	private RandomAccessFile file;
	
	/**
	 * Determines, whether a skipping area is active.
	 */
	private boolean skippingAreaActive = false;
	
	/**
	 * Contains a buffer for write operations in a skipping area.
	 */
	private ByteArrayOutputStream skippingAreaBuffer = new ByteArrayOutputStream();
	

	/**
	 * Creates a new IndexFile instance for the given file.
	 * @param file
	 * @param mode
	 * @throws FileNotFoundException
	 */
	public IndexFile(File file, String mode) throws FileNotFoundException {
		this.file = new RandomAccessFile(file, mode);
	}
	
	
	@Override
	public void read(byte[] bytes) throws IOException {
		this.file.readFully(bytes);
	}
	
	public void write(byte[] bytes) throws IOException {
		if(this.skippingAreaActive) {
			this.skippingAreaBuffer.write(bytes);
		}
		else {
			this.file.write(bytes);
		}
	}
	
	@Override
	public byte readByte() throws IOException {
		return this.file.readByte();
	}
	
	public void writeByte(byte value) throws IOException {
		if(this.skippingAreaActive) {
			this.skippingAreaBuffer.write(value);
		}
		else {
			this.file.writeByte(value);
		}
	}

	@Override
	public short readShort() throws IOException {
		byte[] bytes = new byte[Short.BYTES];
		this.read(bytes);
		return Shorts.fromByteArray(bytes);
	}


	@Override
	public void writeShort(short value) throws IOException {
		byte[] bytes = Shorts.toByteArray(value);
		this.write(bytes);
	}


	@Override
	public int readInt() throws IOException {
		byte[] bytes = new byte[Integer.BYTES];
		this.read(bytes);
		return Ints.fromByteArray(bytes);
	}


	@Override
	public void writeInt(int value) throws IOException {
		byte[] bytes = Ints.toByteArray(value);
		this.write(bytes);
	}


	@Override
	public long readLong() throws IOException {
		byte[] bytes = new byte[Long.BYTES];
		this.read(bytes);
		return Longs.fromByteArray(bytes);
	}


	@Override
	public void writeLong(long value) throws IOException {
		byte[] bytes = Longs.toByteArray(value);
		this.write(bytes);
	}

	@Override
	public String readString() throws IOException {
		int length = this.readInt();
		byte[] bytes = new byte[length];
		this.read(bytes);
		
		return new String(bytes, ENCODING);
	}
	
	@Override
	public void writeString(String string) throws IOException {
		byte[] bytes = string.getBytes(ENCODING);
		this.writeInt(bytes.length);
		this.write(bytes);
	}

	@Override
	public long getFilePointer() throws IOException {
		return this.file.getFilePointer();
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
	public void seek(long pos) throws IOException {
		this.file.seek(pos);
	}
	
	public long length() throws IOException {
		return this.file.length();
	}

	@Override
	public void close() throws IOException {
		if(this.skippingAreaActive) {
			this.flushSkippingArea();
		}
		
		this.file.close();
	}
}
