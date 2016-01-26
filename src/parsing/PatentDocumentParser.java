package parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Joiner;
import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import documents.PatentContentDocument;
import postings.ContentType;

public class PatentDocumentParser implements Iterator<PatentContentDocument>, Iterable<PatentContentDocument> {
	
	/**
	 * XPaths constants for XML elements containing the whole patent, document ID and linked document IDs.
	 */
	private static final String PATENT_PATH = "//us-patent-grant";
	private static final String DOCUMENT_ID_PATH = "us-bibliographic-data-grant/publication-reference/document-id/doc-number";
	private static final String LINKED_DOCUMENT_ID_PATH = "us-bibliographic-data-grant/" + 
															"*[local-name()='references-cited' or local-name()='us-references-cited']/" + 
															"*[local-name()='citation' or local-name()='us-citation']/patcit/" + 
															"document-id[country='US' and date>20110000 and date<20160000 and (kind='B1' or kind='B2')]/" + 
															"doc-number";
	
	/**
	 * Contains the if of the file that is currently parsed.
	 */
	private int fileId;
	
	/**
	 * Contains VTD-XML parser instances.
	 */
	private VTDNav navigation;
	private AutoPilot autoPilot;
	
	/**
	 * Contains the root token of the currently processed patent document.
	 */
	private int currentDocumentToken = -1;
	
	
	/**
	 * Creates a new PatentDocumentParser instance for a file by specifying its path.
	 * @param filePath
	 */
	public PatentDocumentParser(String filePath) {
		this.fileId = Integer.parseInt(FilenameUtils.getBaseName(filePath).substring(3));
		
		VTDGen generator = new VTDGen();
		generator.parseFile(filePath, true);
		
		this.initialize(generator);
	}
	
	/**
	 * Creates a new PatentDocumentParser instance for a given byte array containing a XML document.
	 * @param filePath
	 */
	public PatentDocumentParser(int fileId, byte[] fileBytes) throws ParseException {
		this.fileId = fileId;
		
		VTDGen generator = new VTDGen();
		generator.setDoc(fileBytes);
		generator.parse(true);
		
		this.initialize(generator);
	}
	
	/**
	 * Initialize parser using given generator.
	 * @param generator
	 */
	private void initialize(VTDGen generator) {
		this.navigation = generator.getNav();
		this.autoPilot = new AutoPilot(this.navigation);
		
		// Start iteration over patent documents
		try {
			this.autoPilot.selectXPath(PATENT_PATH);
			this.skipToNextPatent();
		} catch (XPathParseException | NavException e) { }
	}
	

	@Override
	public boolean hasNext() {
		return this.currentDocumentToken != -1;
	}

	@Override
	public PatentContentDocument next() {
		int documentId = -1;
		int offset, length;
		int[] linkedDocuments;
		Map<ContentType, String> contents = new HashMap<ContentType, String>();
		
		this.navigation.push();
		
		try {			
			// Extract document id
			documentId = Integer.parseInt(this.getProperty(DOCUMENT_ID_PATH).get(0));
			
			// Extract different contents
			for(ContentType type: ContentType.values()) {
				String content = this.getDocumentPart(type);
				contents.put(type, content);
			}
			
			// Extract citations
			linkedDocuments = this.getProperty(LINKED_DOCUMENT_ID_PATH).stream()
											.mapToInt(Integer::parseInt)
											.toArray();
			
			// Determine offset and length
			long fragment = this.navigation.getElementFragment();
			offset = (int)fragment;
			length = (int)(fragment >> 32);
			
		} catch (NavException | XPathParseException | XPathEvalException e) {
			throw new NoSuchElementException();
		}
		
		this.navigation.pop();
		
		try {
			this.skipToNextPatent();
		} catch (NavException e) {
			this.currentDocumentToken = -1;
		}
		
		return new PatentContentDocument(documentId, this.fileId, offset, length, contents, linkedDocuments);
	}
	
	/**
	 * Gets a specific part of the current patent.
	 * @param documentPart
	 * @return
	 * @throws XPathParseException
	 * @throws NavException
	 * @throws XPathEvalException
	 */
	private String getDocumentPart(ContentType documentPart) throws XPathParseException, NavException, XPathEvalException {
		return this.getSingleProperty(documentPart.getXPath());
	}
	
	/**
	 * Gets a specific part of the current patent defined by a xpath to its elements.
	 * Matching elements are concatenated.
	 * @param xpath
	 * @return
	 * @throws XPathParseException
	 * @throws NavException
	 * @throws XPathEvalException
	 */
	private String getSingleProperty(String xpath) throws XPathParseException, NavException, XPathEvalException {
		return Joiner.on("").join(this.getProperty(xpath));
	}
	
	/**
	 * Gets specific parts of the current patent defined by a xpath to its elements.
	 * @param xpath
	 * @return
	 * @throws XPathParseException
	 * @throws NavException
	 * @throws XPathEvalException
	 */
	private List<String> getProperty(String xpath) throws XPathParseException, NavException, XPathEvalException {
		// Store navigation context
		this.navigation.push();
		
		// Navigate to first token matching given xpath
		AutoPilot localAutoPilot = new AutoPilot(this.navigation);
		localAutoPilot.selectXPath(xpath);
		List<String> values = new ArrayList<String>();
		while(localAutoPilot.evalXPath() != -1){
			// Get value
			String value = this.navigation.getXPathStringVal();
			values.add(value);
		}
		
		// Restore initial navigation context
		this.navigation.pop();
		
		return values;
	}
	
	/**
	 * Skips to the next patent element.
	 * @throws NavException
	 */
	private void skipToNextPatent() throws NavException {
		try {
			this.currentDocumentToken = this.autoPilot.evalXPath();
		} catch (XPathEvalException e) { }
	}

	@Override
	public Iterator<PatentContentDocument> iterator() {
		return this;
	}
}
