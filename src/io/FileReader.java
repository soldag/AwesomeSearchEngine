package io;

import java.io.IOException;

public interface FileReader extends FileOperator {

	/**
	 * Reads up to b.length bytes of data from this file into an array of bytes.
	 * @param bytes
	 * @throws IOException
	 */
	public void read(byte[] bytes) throws IOException;
	
	/**
	 * Reads a byte of data from this file.
	 * @return
	 * @throws IOException
	 */
	public byte readByte() throws IOException;
	
	/**
	 * Reads a signed 16-bit number from this file.
	 * @return
	 * @throws IOException
	 */
	public short readShort() throws IOException;
	
	/**
	 * Reads a signed 32-bit integer from this file.
	 * @return
	 * @throws IOException
	 */
	public int readInt() throws IOException;
	
	/**
	 * Reads a signed 64-bit integer from this file.
	 * @return
	 * @throws IOException
	 */
	public long readLong() throws IOException;
	
	/**
	 * Reads a string from this file.
	 * @return
	 * @throws IOException
	 */
	public String readString() throws IOException;
}
