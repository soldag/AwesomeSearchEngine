package io.index;

import java.io.IOException;

import io.lowlevel.FileWriter;

public class CompressedIndexWriter extends UncompressedIndexWriter {

	/**
	 * Creates a new CompressedIndexFileWriter instance.
	 * @param fileWriter
	 */
	public CompressedIndexWriter(FileWriter fileWriter) {
		super(fileWriter);
	}
	
	
	@Override
	public IndexWriter uncompressed() {
		return (UncompressedIndexWriter)this;
	}
	
	@Override
	public boolean isCompressed() {
		return true;
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
	public void writeLong(long value) throws IOException {
		while ((value & ~0x7F) != 0) {
			this.writeByte((byte)((value & 0x7f) | 0x80));
			value >>>= 7;
		}
		this.writeByte((byte)value);
	}
}
