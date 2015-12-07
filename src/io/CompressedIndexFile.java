package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CompressedIndexFile extends IndexFile {

	/**
	 * Creates a new CompressedIndexFile instance.
	 * @param file
	 * @param mode
	 * @throws FileNotFoundException
	 */
	public CompressedIndexFile(File file, String mode) throws FileNotFoundException {
		super(file, mode);
	}
	
	
	@Override
	public int readInt() throws IOException {
		byte b = this.readByte();
	    int i = b & 0x7F;
	    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
	    	b = this.readByte();
	    	i |= (b & 0x7F) << shift;
	    }
	    return i;
	}
	
	@Override
	public void writeInt(int value) throws IOException {
		while ((value & ~0x7F) != 0) {
			this.writeByte((byte)((value & 0x7f) | 0x80));
			value >>>= 7;
	    }
		this.writeByte((byte)value);
	}
	
	@Override
	public long readLong() throws IOException {
		byte b = this.readByte();
	    long i = b & 0x7F;
	    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
	    	b = this.readByte();
	    	i |= (b & 0x7FL) << shift;
	    }
	    return i;
	}
	
	@Override
	public void writeLong(long value) throws IOException {
		while ((value & ~0x7F) != 0) {
			this.writeByte((byte)((value & 0x7f) | 0x80));
			value >>>= 7;
		}
		this.writeByte((byte)value);
	}
	
	@Override
	public boolean isCompressed() {
		return true;
	}
}
