package parsers;

import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import org.javatuples.Pair;

public class PatentTitleLookup extends PatentParser<Pair<String, String>> {
	
	/**
	 * XPaths constant for XML elements containing the title of a patent.
	 */
	protected static final String TITLE_PATH = PATENT_PATH + "/us-bibliographic-data-grant/invention-title";
	
	/**
	 * Creates a new instance.
	 * @param inputStream
	 * @throws XMLStreamException
	 */
	public PatentTitleLookup(InputStream inputStream) throws XMLStreamException {
		super(inputStream);
	}

	/**
	 * Creates a new instance.
	 * @param reader
	 * @throws XMLStreamException
	 */
	public PatentTitleLookup(Reader reader) throws XMLStreamException {
		super(reader);
	}
	
	
	/**
	 * Processes each XML event in order to extract the title.
	 * @param event
	 * @return Pair of document id and title, if it was extracted completely. Null, if title element was not reached, yet.
	 */
	protected Pair<String, String> processEvent(XMLEvent event) {
		if (event.getEventType() == XMLStreamReader.CHARACTERS && this.getCurrentPath().equals(TITLE_PATH)) {
			Characters characters = event.asCharacters();
			return new Pair<String, String>(this.getCurrentDocumentId(), characters.toString());
		}
		
		return null;
	}
	
	/**
	 * Looks up the title of a specific patent identified by its document id.
	 * @param documentId
	 * @return Title of the specified document. If document cannot be found, null is returned.
	 * @throws XMLStreamException
	 */
	public String getTitle(String documentId) throws XMLStreamException {
		Map<String, String> result = this.getTitles(Arrays.asList(documentId));
		if(result.containsKey(documentId)) {
			return result.get(documentId);
		}
		
		return null;
	}
	
	/**
	 * Looks up the titles of specific patents identified by their document ids.
	 * @param documentIds
	 * @return Title of specified documents mapped to its document ids.
	 * @throws XMLStreamException
	 */
	public Map<String, String> getTitles(List<String> documentIds) throws XMLStreamException {
		Map<String, String> result = new HashMap<String, String>(documentIds.size());
		while(this.hasNext()) {
			Pair<String, String> idTitleTuple = this.next();
			if(documentIds.contains(idTitleTuple.getValue0())) {
				result.put(idTitleTuple.getValue0(), idTitleTuple.getValue1());
			}
		}
		
		return result;
	}
}
