
package SearchEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/*
* use the function 'ArrayList <String> getGoogleRanking(String query)' to get the gold rankings from google for a given query and compute NDCG later on
* this function returns the patent IDs that google returned for this query
* the result set will be at most 100 US utility patent grants from 2011 to 2015
*/

public class WebFile {

    // returns at most 100 patent IDs
    public ArrayList <String> getGoogleRanking(String query) {        
        // only US : &tbs=ptso:us
        // only US grants : &tbs=ptso:us,ptss:g
        // only US utility grants : &tbs=ptso:us,ptss:g,ptst:u
    	// only US utility grants published between 2011 and 2015: &tbs=ptso:us,ptss:g,ptst:u,cdr:1,cd_min:01.01.2011,cd_max:31.12.2015,ptsdt:i
        
        String minID = "7861317"; // 2011
        String maxID = "8984661"; // 2015
        
        ArrayList <String> ranking = new ArrayList <>();
        int safeNumber = 100;  // to get enough US utility patents and exclude others
        try {
            // issue the query
            String queryTerms = query.replaceAll(" ", "+").replace("NOT ", "-");
            String queryUrl = "https://www.google.com/search?hl=en&q=" + queryTerms + "&tbm=pts&num=" + safeNumber + "&tbs=ptso:us,ptss:g,ptst:u,cdr:1,cd_min:01.01.2011,cd_max:31.12.2015,ptsdt:i";
            
            java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
            try(WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
	            HtmlPage htmlPage = webClient.getPage(queryUrl);
	            for(DomElement h3: htmlPage.getElementsByTagName("h3")) {
	            	DomElement link = h3.getElementsByTagName("a").get(0);
	            	String url = link.getAttribute("href");
	            	Pattern patentPattern = Pattern.compile("/patents/US(\\d+)\\?dq=");
	                Matcher patentMatcher = patentPattern.matcher(url);
	                String patentNumber = "";
	                while (patentMatcher.find()) {
	                    patentNumber = patentMatcher.group(1); // get the ID
	                }
	                if(patentNumber!= null && patentNumber.compareTo(minID) > 0 && patentNumber.compareTo(maxID) < 0) {
	                    ranking.add(patentNumber.replace("?", "")); // without the zero infront of the ID
	                }
	            }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(WebFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ranking;
    }
}
