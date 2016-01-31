package io.index;

import java.io.IOException;

import io.lowlevel.FileReader;

public interface IndexReader extends FileReader {

	/**
	 * Determines, whether the data is stored compressed in the file.
	 * @return
	 */
	public boolean isCompressed();
	
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
	 * Reads a float from this file
	 * @return
	 */
	public float readFloat() throws IOException;
	
	/**
	 * Reads a double from this file.
	 * @return
	 */
	public double readDouble() throws IOException;
	
	/**
	 * Reads a string from this file.
	 * @return
	 * @throws IOException
	 */
	public String readString() throws IOException;
	
	/**
	 * Gets the length of the starting skipping area.
	 * @return
	 */
	public int getSkippingAreaLength() throws IOException;
	
	/**
	 * Skips the starting skipping area.
	 */
	public void skipSkippingArea() throws IOException;
	
	/**
	 * Loads the skipping area in memory and returns a reader for it.
	 * @return
	 * @throws IOException
	 */
	public IndexReader getSkippingAreaReader() throws IOException;
}
