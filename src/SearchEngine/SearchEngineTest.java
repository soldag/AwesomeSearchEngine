package SearchEngine;

import java.util.ArrayList;

/**
 *
 * @author: Henriette Dinger & Soeren Oldag
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 *
 * You can run your search engine using this file
 * You can use/change this file during the development of your search engine.
 * Any changes you make here will be ignored for the final test!
 */

public class SearchEngineTest {
    
    
    public static void main(String args[]) throws Exception {
    	long start,time;
    	String dataDirectory = "/Users/soldag/Copy/Studium/Master/1. Semester/Information Retrieval & Web Search/Excercises/Data";
        SearchEngine engine = new AwesomeSearchEngine();
        
        start = System.currentTimeMillis();
        engine.compressIndex(dataDirectory);
        time = System.currentTimeMillis() - start;
        
        System.out.println("Indexing Time: " + time + "ms");

        start = System.currentTimeMillis();
        engine.loadCompressedIndex(dataDirectory);
        time = System.currentTimeMillis() - start;
        System.out.println("Loading Index Time: " + time + "ms");
        
        start = System.currentTimeMillis();
        String query = "\"a scanning\"";
        ArrayList<String> results = engine.search(query, 10, 0);
        time = System.currentTimeMillis() - start;
        System.out.println("Search Time: " + time + "ms");
        
        start = System.currentTimeMillis();
        WebFile webFile = new WebFile();
        ArrayList<String> goldRanking = webFile.getGoogleRanking(query);
        time = System.currentTimeMillis() - start;
        System.out.println("Gold Ranking Retrieval Time: " + time + "ms");
        
        System.out.println();
        System.out.println("Results:");
        for(int i = 0; i < results.size(); i++) {
        	System.out.println(results.get(i));
        	
        	double ndcg = engine.computeNdcg(goldRanking, results, i + 1);
        	System.out.println(String.format("NDCG: %.2f", ndcg));
        	System.out.println();
        }    
    }

}
