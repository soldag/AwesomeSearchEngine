package querying.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.sparse.CCSMatrix;
import org.la4j.vector.dense.BasicVector;

import documents.PatentDocument;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;

public class PageRankCalculator {
	
	/**
	 * Contains the damping parameter for page rank calculation.
	 */
	private static final double DAMPING_FACTOR = 0.85;
	
	/**
	 * Contains the number of iterations that should be done when calculating page ranks.
	 */
	private static final double ITERATIONS_COUNT = 45;
	
	
	/**
	 * Calculates the page rank score for each of the given documents.
	 * @param linkedDocuments
	 * @return
	 */
	public List<PatentDocument> calculate(Map<PatentDocument, TIntList> linkedDocuments) {
		// Create id-document mapping and extract min and max document id
		int minDocumentId = Integer.MAX_VALUE;
		int maxDocumentId = Integer.MIN_VALUE;
		TIntObjectMap<PatentDocument> documentIdMapping = new TIntObjectHashMap<PatentDocument>();
		for(PatentDocument document: linkedDocuments.keySet()) {
			int documentId = document.getId();
			documentIdMapping.put(documentId, document);
			
			if(documentId > maxDocumentId) {
				maxDocumentId = documentId;
			}
			if(documentId < minDocumentId) {
				minDocumentId = documentId;
			}
		}
		
		// Initialize adjacency matrix
		final int finalMinDocumentId = minDocumentId;
		int documentsCount = linkedDocuments.keySet().size();
		int matrixSize = maxDocumentId - minDocumentId;
		Matrix adjacencyMatrix = new CCSMatrix(matrixSize, matrixSize);
		for(Map.Entry<PatentDocument, TIntList> entry: linkedDocuments.entrySet()) {
			int j = entry.getKey().getId() - minDocumentId;
			TIntList linkedDocumentIds = entry.getValue();
			double citationsCount = linkedDocumentIds.size();
			linkedDocumentIds.forEach(new TIntProcedure() {
				@Override
				public boolean execute(int documentId) {
					int i = documentId - finalMinDocumentId;
					if(i < 0) {
						return false;
					}
					adjacencyMatrix.set(i, j, 1/citationsCount);
					return true;
				}
			});
		}
		
		// Initialize page rank and damping vectors
		Vector pageRankVector = BasicVector.constant(matrixSize, 1d/documentsCount);
		Vector dampingVector = BasicVector.constant(matrixSize, (1 - DAMPING_FACTOR)/documentsCount);
		
		// Calculate page rank iteratively
		for(int i = 0; i < ITERATIONS_COUNT; i++) {
			pageRankVector = dampingVector.add(adjacencyMatrix.multiply(pageRankVector).multiply(DAMPING_FACTOR));
		}
		
		// Assign page ranks to documents
		List<PatentDocument> documents = new ArrayList<PatentDocument>();
		for(int i = 0; i < matrixSize; i++) {
			int documentId = i + minDocumentId;
			PatentDocument document = documentIdMapping.get(documentId);
			if(document != null) {
				document.setPageRank(pageRankVector.get(i));
				documents.add(document);
			}
		}
		
		return documents;
	}
}
