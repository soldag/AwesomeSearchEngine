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
        engine.index(dataDirectory);
        time = System.currentTimeMillis() - start;
        
        System.out.println("Indexing Time: " + time + "ms");

        start = System.currentTimeMillis();
        engine.loadIndex(dataDirectory);
        time = System.currentTimeMillis() - start;
        System.out.println("Loading Index Time: " + time + "ms");
        
        start = System.currentTimeMillis();
        String query = "selection";
        ArrayList<String> results = engine.search(query, 0, 0);
        time = System.currentTimeMillis() - start;
        System.out.println("Search Time: " + time + "ms");
        
        System.out.println();
        System.out.println("Results:");
        for(String title: results) {
        	System.out.println(title);
        }
    }

}
