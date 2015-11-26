package parsing.parsers;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;
import com.ximpleware.extended.AutoPilotHuge;
import com.ximpleware.extended.NavExceptionHuge;
import com.ximpleware.extended.TextIter;
import com.ximpleware.extended.VTDGenHuge;
import com.ximpleware.extended.VTDNavHuge;
import com.ximpleware.extended.XPathEvalExceptionHuge;
import com.ximpleware.extended.XPathParseExceptionHuge;

import parsing.PatentContentDocument;

public class PatentDocumentParser implements Iterator<PatentContentDocument>, Iterable<PatentContentDocument> {
	
	/**
	 * XPaths constants for XML elements containing the document ID, title and abstract of a patent.
	 */
	private static final String PATENT_PATH = "/my-root/us-patent-grant";
	private static final String DOCUMENT_ID_PATH = "us-bibliographic-data-grant/publication-reference/document-id/doc-number";
	private static final String TITLE_PATH = "us-bibliographic-data-grant/invention-title";
	private static final String ABSTRACT_PATH = "abstract";
	
	/**
	 * Contains the path of the file that is currently parsed.
	 */
	private String filePath;
	
	/**
	 * Contains VTD-XML parser instances.
	 */
	private VTDNavHuge navigation;
	private AutoPilotHuge autoPilot;
	
	/**
	 * Contains the token that is currently processed.
	 */
	private int currentToken = -1;
	
	
	/**
	 * Creates a new PatentDocumentParser instance.
	 * @param filePath
	 */
	public PatentDocumentParser(String filePath) {
		this.filePath = filePath;
		
		// Initialize VTD-XML
		VTDGenHuge gen = new VTDGenHuge();
		gen.parseFile(this.filePath, true, VTDGenHuge.MEM_MAPPED);
		this.navigation = gen.getNav();
		this.autoPilot = new AutoPilotHuge(this.navigation);
		
		// Start iteration over patent documents
		try {
			this.autoPilot.selectXPath(PATENT_PATH);
			this.skipToNextPatent();
		} catch (XPathParseExceptionHuge | NavExceptionHuge e) { }
	}
	

	@Override
	public boolean hasNext() {
		return this.currentToken != -1;
	}

	@Override
	public PatentContentDocument next() {
		int documentId = -1;
		String abstractText = null;
		String title = null;
		int fileId = Integer.parseInt(FilenameUtils.getBaseName(this.filePath).substring(3));
		long titleOffset = -1, abstractOffset = -1;
		int titleLength = -1, abstractLength = -1;
		
		this.navigation.push();
		
		try {
			// Extract document id
			PatentDocumentProperty documentIdProperty = this.getProperty(DOCUMENT_ID_PATH);
			documentId = Integer.parseInt(documentIdProperty.getValue());
			
			// Extract title
			PatentDocumentProperty titleProperty = this.getProperty(TITLE_PATH);
			title = titleProperty.getValue();
			titleOffset = titleProperty.getOffset();
			titleLength = titleProperty.getLength();			
			
			// Extract abstract
			PatentDocumentProperty abstractProperty = this.getProperty(ABSTRACT_PATH);
			abstractText = abstractProperty.getValue();
			abstractOffset = abstractProperty.getOffset();
			abstractLength = abstractProperty.getLength();
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge e) {
			throw new NoSuchElementException();
		}
		
		this.navigation.pop();
		try {
			this.skipToNextPatent();
		} catch (NavExceptionHuge e) {
			this.currentToken = -1;
		}
		
		return new PatentContentDocument(documentId, fileId, titleOffset, titleLength, abstractOffset, abstractLength, title, abstractText);
	}
	
	/**
	 * Gets the value of a patent property of the current patent defined by a xpath to its elements.
	 * @param xpath
	 * @return
	 * @throws XPathParseExceptionHuge
	 * @throws NavExceptionHuge
	 * @throws XPathEvalExceptionHuge
	 */
	private PatentDocumentProperty getProperty(String xpath) throws XPathParseExceptionHuge, NavExceptionHuge, XPathEvalExceptionHuge {
		// Store navigation context
		this.navigation.push();
		
		// Navigate to first token matching given xpath
		AutoPilotHuge localAutoPilot = new AutoPilotHuge(this.navigation);
		localAutoPilot.selectXPath(xpath);
		int token = localAutoPilot.evalXPath();
		if(token == -1) {
			return null;
		}
		
		// Get positional information
		long offset = this.navigation.getTokenOffset(token) - 1;
		
		// Get value
		String value = this.extractMixedText();
		this.navigation.toElement(VTDNavHuge.NEXT_SIBLING);
		int length = (int)(this.navigation.getTokenOffset(this.navigation.getCurrentIndex()) - offset - 1);
		
		// Restore initial navigation context
		this.navigation.pop();
		
		return new PatentDocumentProperty(value, offset, length);
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
		this.navigation.toElement(VTDNavHuge.FIRST_CHILD);
		TextIter iter = new TextIter();
		iter.touch(navigation);
		
		// Concat text of following text and child nodes of the current element
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
			this.currentToken = this.autoPilot.evalXPath();
		} catch (XPathEvalExceptionHuge e) { }
	}

	@Override
	public Iterator<PatentContentDocument> iterator() {
		return this;
	}
}
