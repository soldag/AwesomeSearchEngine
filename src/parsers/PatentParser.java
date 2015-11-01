package parsers;

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

public abstract class PatentParser<T> implements Iterator<T>, Iterable<T> {
	
	/**
	 * XPaths constants for XML elements containing the document ID and abstract of a patent.
	 */
	protected static final String PATENT_PATH = "my-root/us-patent-grant";
	protected static final String DOCUMENT_ID_PATH = PATENT_PATH + "/us-bibliographic-data-grant/publication-reference/document-id/doc-number";
	
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
	 * Contains the id of the currently processing patent.
	 */
	private Integer currentDocumentId = null;
	
	
	/**
	 * Creates a new instance.
	 * @param inputStream
	 * @throws XMLStreamException
	 */
	public PatentParser(InputStream inputStream) throws XMLStreamException {
		XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
		this.xmlParser = xmlFactory.createXMLEventReader(inputStream);
		this.skipToNextPatent();
	}
	
	/**
	 * Creates a new instance.
	 * @param reader
	 * @throws XMLStreamException
	 */
	public PatentParser(Reader reader) throws XMLStreamException {
		XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
		this.xmlParser = xmlFactory.createXMLEventReader(reader);
		this.skipToNextPatent();	
	}
	
	
	/**
	 * Gets the path of the currently processed XML element of the source file.
	 * @return String
	 */
	protected String getCurrentPath() {
		return this.currentPath;
	}
	
	/**
	 * Gets the id of the currently processing patent. 
	 * @return Integer
	 */
	protected Integer getCurrentDocumentId() {
		return this.currentDocumentId;
	}
	

	/**
	 * Returns true if the iteration has more elements.
	 */
	public boolean hasNext() {
		return this.hasNext;
	}

	/**
	 * Returns the next element in the iteration.
	 */
	public T next() {
		T result = null;
		try {
			while (this.xmlParser.hasNext())
			{
				XMLEvent event = this.nextXmlEvent();
				
				result = this.processEvent(event);
				if(result != null) {
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
		
		return result;
	}
	
	/**
	 * Processes each XML event. Has to be implemented by inherited class.
	 * @param event
	 * @return Element of type T, if wanted information are extracted from document yet and can be returned. Null, if next event has to be processed.
	 */
	protected abstract T processEvent(XMLEvent event);
	
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
				this.currentDocumentId = null;
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
	protected XMLEvent nextXmlEvent() throws XMLStreamException {
		XMLEvent event = this.xmlParser.nextEvent();
		this.updatePath(event);
		this.updateDocumentId(event);
		
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
	
	
	/**
	 * Updates document ID. Has to be called for each XML event.
	 */
	private void updateDocumentId(XMLEvent event) {
		if(event.getEventType() == XMLStreamConstants.CHARACTERS && this.currentPath.equals(DOCUMENT_ID_PATH)) {
			Characters characters = event.asCharacters();
			this.currentDocumentId = Integer.parseInt(characters.toString());
		}
	}
	
	
	/**
	 * Frees any resources associated with this Reader. This method does not close the underlying input source.
	 */
	public void close() {
		this.currentPath = null;
		this.currentDocumentId = null;
		
		try {
			this.xmlParser.close();
		} catch (XMLStreamException e) { }
	}

	@Override
	/**
	 * Returns an iterator over a set of elements of type T.
	 */
	public Iterator<T> iterator() {
		return this;
	}
}
