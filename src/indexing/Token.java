package indexing;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Token {
	
	/**
	 * Contains the encoding used for writing strings to file.
	 */
	private static final String ENCODING = "UTF-8";

	/**
	 * Reads a token from a given DataInput.
	 * @param input
	 * @return Token
	 * @throws IOException
	 */
	public static String read(DataInput input) throws IOException {
		int length = input.readInt();
		byte[] bytes = new byte[length];
		input.readFully(bytes);
		
		return new String(bytes, ENCODING);
	}
	
	/**
	 * Writes a token to a given DataOutput.
	 * @param token
	 * @param output
	 * @throws IOException
	 */
	public static void write(String token, DataOutput output) throws IOException {
		byte[] bytes = token.getBytes(ENCODING);
		output.writeInt(bytes.length);
		output.write(bytes);
	}
}