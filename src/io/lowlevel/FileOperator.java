package io.lowlevel;

import java.io.Closeable;
import java.io.IOException;

public interface FileOperator extends Closeable {
	
	/**
	 * Returns the current offset in this file.
	 * @return
	 * @throws IOException
	 */
	public long getFilePointer() throws IOException;
	
	/**
	 * Sets the file-pointer offset, measured from the beginning of this file, at which the next read or write occurs.
	 * @param pos
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException;
	
	/**
	 * Returns the length of this file.
	 * @return
	 * @throws IOException
	 */
	public long length() throws IOException;
}
