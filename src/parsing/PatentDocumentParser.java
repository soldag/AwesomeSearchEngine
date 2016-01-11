package parsing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;

import com.ximpleware.extended.AutoPilotHuge;
import com.ximpleware.extended.NavExceptionHuge;
import com.ximpleware.extended.ParseExceptionHuge;
import com.ximpleware.extended.TextIter;
import com.ximpleware.extended.VTDGenHuge;
import com.ximpleware.extended.VTDNavHuge;
import com.ximpleware.extended.XMLBuffer;
import com.ximpleware.extended.XPathEvalExceptionHuge;
import com.ximpleware.extended.XPathParseExceptionHuge;

import documents.PatentContentDocument;
import postings.ContentType;

public class PatentDocumentParser implements Iterator<PatentContentDocument>, Iterable<PatentContentDocument> {
	
	/**
	 * XPaths constants for XML elements containing the whole patent and its document ID.
	 */
	private static final String PATENT_PATH = "//us-patent-grant"; //TODO: affects performance?
	private static final String DOCUMENT_ID_PATH = "us-bibliographic-data-grant/publication-reference/document-id/doc-number";
	
	/**
	 * Contains the if of the file that is currently parsed.
	 */
	private int fileId;
	
	/**
	 * Contains VTD-XML parser instances.
	 */
	private VTDNavHuge navigation;
	private AutoPilotHuge autoPilot;
	
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
		
		VTDGenHuge generator = new VTDGenHuge();
		generator.parseFile(filePath, true, VTDGenHuge.MEM_MAPPED);
		
		this.initialize(generator);
	}
	
	/**
	 * Creates a new PatentDocumentParser instance for a given byte array containing a XML document.
	 * @param filePath
	 */
	public PatentDocumentParser(int fileId, byte[] fileBytes) throws ParseExceptionHuge {
		this.fileId = fileId;
		
		VTDGenHuge generator = new VTDGenHuge();
		XMLBuffer buffer = new XMLBuffer(fileBytes);
		generator.setDoc(buffer);
		generator.parse(true);
		
		this.initialize(generator);
	}
	
	/**
	 * Initialize parser using given generator.
	 * @param generator
	 */
	private void initialize(VTDGenHuge generator) {
		this.navigation = generator.getNav();
		this.autoPilot = new AutoPilotHuge(this.navigation);
		
		// Start iteration over patent documents
		try {
			this.autoPilot.selectXPath(PATENT_PATH);
			this.skipToNextPatent();
		} catch (XPathParseExceptionHuge | NavExceptionHuge e) { }
	}
	

	@Override
	public boolean hasNext() {
		return this.currentDocumentToken != -1;
	}

	@Override
	public PatentContentDocument next() {
		int documentId = -1;
		long offset;
		int length;
		Map<ContentType, String> contents = new HashMap<ContentType, String>();
		
		this.navigation.push();
		
		try {			
			// Extract document id
			documentId = Integer.parseInt(this.getProperty(DOCUMENT_ID_PATH));
			
			// Extract different contents
			for(ContentType type: ContentType.values()) {
				String content = this.getDocumentPart(type);
				contents.put(type, content);
			}
			
			// Determine offset and length
			long[] fragment = this.navigation.getElementFragment();
			offset = fragment[0];
	        length = (int)fragment[1];
			
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge e) {
			throw new NoSuchElementException();
		}
		
		this.navigation.pop();
		
		try {
			this.skipToNextPatent();
		} catch (NavExceptionHuge e) {
			this.currentDocumentToken = -1;
		}
		
		return new PatentContentDocument(documentId, this.fileId, offset, length, contents);
	}
	
	/**
	 * Gets a specific part of the current patent.
	 * @param documentPart
	 * @return
	 * @throws XPathParseExceptionHuge
	 * @throws NavExceptionHuge
	 * @throws XPathEvalExceptionHuge
	 */
	private String getDocumentPart(ContentType documentPart) throws XPathParseExceptionHuge, NavExceptionHuge, XPathEvalExceptionHuge {
		return this.getProperty(documentPart.getXPath());
	}
	
	/**
	 * Gets a specific part of the current patent defined by a xpath to its elements.
	 * @param xpath
	 * @return
	 * @throws XPathParseExceptionHuge
	 * @throws NavExceptionHuge
	 * @throws XPathEvalExceptionHuge
	 */
	private String getProperty(String xpath) throws XPathParseExceptionHuge, NavExceptionHuge, XPathEvalExceptionHuge {
		// Store navigation context
		this.navigation.push();
		
		// Navigate to first token matching given xpath
		AutoPilotHuge localAutoPilot = new AutoPilotHuge(this.navigation);
		localAutoPilot.selectXPath(xpath);
		StringBuilder valueBuilder = new StringBuilder();
		while(localAutoPilot.evalXPath() != -1){
			// Get value
			String value = this.extractMixedText();
			valueBuilder.append(value);
		}
		
		// Restore initial navigation context
		this.navigation.pop();
		
		if(valueBuilder.length() == 0) {
			return null;
		}		
		return valueBuilder.toString();
	}
	
	/**
	 * Reads text of the current token and its children. 
	 * @return
	 * @throws NavExceptionHuge
	 */
	private String extractMixedText() throws NavExceptionHuge {
		// Store navigation context
		this.navigation.push();
		
		// Create iterator for texts
		TextIter iter = new TextIter();
		iter.touch(navigation);
		
		// Concatenate text of following text and child nodes of the current element
		int i;
		StringBuilder textBuilder = new StringBuilder();
		while ((i = iter.getNext()) != -1) {
			textBuilder.append(this.navigation.toString(i));
			
			if(this.navigation.toElement(VTDNavHuge.FIRST_CHILD)) {
				textBuilder.append(this.extractMixedText());
			}
		}
		
		// Restore initial navigation context, if asked for
		this.navigation.pop();
		
		return textBuilder.toString();
	}
	
	/**
	 * Skips to the next patent element.
	 * @throws NavExceptionHuge
	 */
	private void skipToNextPatent() throws NavExceptionHuge {
		try {
			this.currentDocumentToken = this.autoPilot.evalXPath();
		} catch (XPathEvalExceptionHuge e) { }
	}

	@Override
	public Iterator<PatentContentDocument> iterator() {
		return this;
	}
}
