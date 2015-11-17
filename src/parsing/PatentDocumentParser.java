package parsing;

import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import SearchEngine.PatentAbstractDocument;

public class PatentDocumentParser implements Iterator<PatentAbstractDocument>, Iterable<PatentAbstractDocument>, AutoCloseable {
	
	/**
	 * XPaths constants for XML elements containing the document ID, title and abstract of a patent.
	 */
	private static final String PATENT_PATH = "my-root/us-patent-grant";
	private static final String DOCUMENT_ID_PATH = PATENT_PATH + "/us-bibliographic-data-grant/publication-reference/document-id/doc-number";
	private static final String TITLE_PATH = PATENT_PATH + "/us-bibliographic-data-grant/invention-title";
	private static final String ABSTRACT_PATH = PATENT_PATH + "/abstract";
	
	/**
	 * Contains the underlying XML parser.
	 */
	private XMLEventReader xmlParser;

	/**
	 * Determines, whether there are more patents to parse.
	 */
	private boolean hasNext;
	
	/**
	 * Contains the node path of the current XML element.
	 */
	private String currentPath = "";
	
	
	/**
	 * Creates a new instance.
	 * @param inputStream
	 * @throws XMLStreamException
	 */
	public PatentDocumentParser(InputStream inputStream) throws XMLStreamException {
		XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
		this.xmlParser = xmlFactory.createXMLEventReader(inputStream);
		this.skipToNextPatent();
	}
	
	
	/**
	 * Creates a new instance.
	 * @param reader
	 * @throws XMLStreamException
	 */
	public PatentDocumentParser(Reader reader) throws XMLStreamException {
		XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
		this.xmlParser = xmlFactory.createXMLEventReader(reader);
		this.skipToNextPatent();	
	}
	

	@Override
	public boolean hasNext() {
		return this.hasNext;
	}

	@Override
	public PatentAbstractDocument next() {
		int documentId = -1;
		String title = null;
		StringBuilder abstractBuilder = new StringBuilder();
		try {
			while (this.xmlParser.hasNext())
			{
				XMLEvent event = this.nextXmlEvent();
				if(event.getEventType() == XMLStreamConstants.CHARACTERS) {
					Characters characters = event.asCharacters();
					if(this.currentPath.equals(DOCUMENT_ID_PATH)) {
						documentId = Integer.parseInt(characters.toString());
					}
					else if(this.currentPath.equals(TITLE_PATH)) {
						title = characters.toString();
					}
					else if(this.currentPath.startsWith(ABSTRACT_PATH)) {
						abstractBuilder.append(characters.toString());
					}
				}
				else if(event.getEventType() == XMLStreamConstants.END_ELEMENT && this.currentPath.equals(ABSTRACT_PATH)) {
					// All necessary information have been read.
					break;
				}
			}
		} catch (XMLStreamException e) {
			throw new NoSuchElementException(e.getMessage());
		}

		try {
			this.skipToNextPatent();
		} catch (XMLStreamException e) { 
			this.hasNext = false;
		}
		
		return new PatentAbstractDocument(documentId, title, abstractBuilder.toString());
	}
	
	/**
	 * Skips position of the XML parser to the start of the next patent element and updates hasNext-field.
	 * @return boolean
	 * @throws XMLStreamException
	 */
	public boolean skipToNextPatent() throws XMLStreamException {
		while (this.xmlParser.hasNext())
		{
			XMLEvent event = this.nextXmlEvent();
			
			if(this.currentPath.equals(PATENT_PATH) && event.getEventType() == XMLStreamConstants.START_ELEMENT) {
				return this.hasNext = true;
			}
		}
		
		return this.hasNext = false;
	}
	
	/**
	 * Gets next parsing event and updates node path accordingly
	 * @returns XMLEvent
	 * @throws XMLStreamException
	 */
	private XMLEvent nextXmlEvent() throws XMLStreamException {
		XMLEvent event = this.xmlParser.nextEvent();
		this.updatePath(event);
		
		return event;
	}
	
	/**
	 * Updates node path to the current XML element. Has to be called for each XML event.
	 */
	private void updatePath(XMLEvent event) {
		int eventType = event.getEventType();
		if(eventType == XMLStreamConstants.START_ELEMENT) {
			// Update current path
			StartElement element = event.asStartElement();
			if(this.currentPath != null && !currentPath.isEmpty()) 
	    	{
				this.currentPath += "/";
	    	}
			this.currentPath += element.getName();
		}
		else if(eventType == XMLStreamConstants.END_ELEMENT) {
			// Update current path
			int index = currentPath.lastIndexOf("/");
	    	if(index == -1) 
	    	{
	    		this.currentPath = "";
	    	}
	    	else
	    	{
	    		this.currentPath = this.currentPath.substring(0, index);
	    	}
		}
	}

	@Override
	public Iterator<PatentAbstractDocument> iterator() {
		return this;
	}
	
	
	/**
	 * Frees any resources associated with this Reader. This method does not close the underlying input source.
	 */
	public void close() {
		this.currentPath = null;
		
		try {
			this.xmlParser.close();
		} catch (XMLStreamException e) { }
	}
}
