package io.lowlevel;

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
}
