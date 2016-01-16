package io.index;

import java.io.IOException;

import io.lowlevel.FileReader;

public class CompressedIndexReader extends UncompressedIndexReader {
	
	/**
	 * Creates a new CompressedIndexReader instance.
	 * @param fileReader
	 */
	public CompressedIndexReader(FileReader fileReader) {
		super(fileReader);
	}
	
	@Override
	public boolean isCompressed() {
		return true;
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
	public long readLong() throws IOException {
		byte b = this.readByte();
	    long i = b & 0x7F;
	    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
	    	b = this.readByte();
	    	i |= (b & 0x7FL) << shift;
	    }
	    return i;
	}
}
