package io.lowlevel;

import java.io.IOException;

public interface FileReader extends FileOperator {

	/**
	 * Reads up to b.length bytes of data from this file into an array of bytes.
	 * @param bytes
	 * @throws IOException
	 */
	public int read(byte[] bytes) throws IOException;
	
	/**
	 * Reads a byte of data from this file.
	 * @return
	 * @throws IOException
	 */
	public byte readByte() throws IOException;
	
	/**
	 * Reads complete file from current position to end.
	 */
	public byte[] readToEnd() throws IOException;
}
