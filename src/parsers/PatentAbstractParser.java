package parsers;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import org.javatuples.Pair;

public class PatentAbstractParser extends PatentParser<Pair<Integer, String>> {
	
	/**
	 * XPaths constant for XML elements containing the abstract of a patent.
	 */
	protected static final String ABSTRACT_PATH = PATENT_PATH + "/abstract";
	
	/**
	 * StringBuilder used for constructing the abstract of an patent, which might be contained in several XML elements.
	 */
	private StringBuilder abstractBuilder = new StringBuilder();
	
	/**
	 * Creates a new instance.
	 * @param inputStream
	 * @throws XMLStreamException
	 */
	public PatentAbstractParser(InputStream inputStream) throws XMLStreamException {
		super(inputStream);
	}

	/**
	 * Creates a new instance.
	 * @param reader
	 * @throws XMLStreamException
	 */
	public PatentAbstractParser(Reader reader) throws XMLStreamException {
		super(reader);
	}
	
	
	/**
	 * Processes each XML event in order to extract the abstract.
	 * @param event
	 * @return Pair of document id and abstract text, if it was extracted completely. Null, if abstract element was not reached or abstract was not read to end, yet.
	 */
	protected Pair<Integer, String> processEvent(XMLEvent event) {
		int eventType = event.getEventType();
		if (eventType == XMLStreamConstants.CHARACTERS && this.getCurrentPath().startsWith(ABSTRACT_PATH)) {
    		// Append (next) part of the abstract text.
			Characters characters = event.asCharacters();
    		this.abstractBuilder.append(characters.toString().trim());
		}
		else if (eventType == XMLStreamConstants.END_ELEMENT && this.getCurrentPath().equals(ABSTRACT_PATH)) {
			String abstractText = this.abstractBuilder.toString();
			this.abstractBuilder = new StringBuilder();
			
			return new Pair<Integer, String>(this.getCurrentDocumentId(), abstractText);
		}
		
		return null;
	}
}
