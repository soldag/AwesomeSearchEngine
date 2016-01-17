package io.index;

import java.io.IOException;

import io.lowlevel.FileWriter;

public interface IndexWriter extends FileWriter {

	/**
	 * Determines, whether the data is stored compressed in the file.
	 * @return
	 */
	public boolean isCompressed();
	
	/**
	 * Writes a short to the file as two bytes, high byte first.
	 * @param value
	 * @throws IOException
	 */
	public void writeShort(short value) throws IOException;
	
	/**
	 * Writes an int to the file as four bytes, high byte first.
	 * @param value
	 * @throws IOException
	 */
	public void writeInt(int value) throws IOException;
	
	/**
	 * Writes a long to the file as eight bytes, high byte first.
	 * @param value
	 * @throws IOException
	 */
	public void writeLong(long value) throws IOException;
	
	/**
	 * Writes a string to the file.
	 * @param string
	 * @throws IOException
	 */
	public void writeString(String string) throws IOException;
	
	
	/**
	 * Starts a skipping area. The length of the following write operations (until end of skipping area) are counted 
	 * and written to the current file position before the actual data. Is used to skip over large parts of the file.
	 * @throws IOException
	 */
	public void startSkippingArea() throws IOException;
	
	/**
	 * Ends a skipping area and flushes the buffered write operations to file.
	 * @throws IOException
	 */
	public void endSkippingArea() throws IOException;
}
