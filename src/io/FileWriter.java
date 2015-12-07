package io;

import java.io.IOException;

public interface FileWriter extends FileOperator {

	/**
	 * Writes b.length bytes from the specified byte array to this file, starting at the current file pointer.
	 * @param bytes
	 * @throws IOException
	 */
	public void write(byte[] bytes) throws IOException;
	
	/**
	 * Writes the specified byte to this file.
	 * @param value
	 * @throws IOException
	 */
	public void writeByte(byte value) throws IOException;
	
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
