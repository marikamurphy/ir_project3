package ir.webutils;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * computes the PageRanks of the spidered pages based on their link structure
 *
 */
public class PageRankSpider extends Spider {

  /**
   * A graph representing the link structure of the pages
   */
  protected Graph linkGraph = new Graph();


  /**
   * Checks command line arguments, performs the crawl and computes page ranks.
   *  <p> This implementation calls <code>processArgs</code>,
   * <code>doCrawl</code>, and <code>computerPageRanks</code>.
   *
   * @param args Command line arguments.
   */
  public void go(String[] args) {
    processArgs(args);
    doCrawl();
    computePageRanks();
  }

  /**
   * Performs the crawl.  Should be called after
   * <code>processArgs</code> has been called.  Assumes that
   * starting url has been set.  <p> This implementation iterates
   * through a list of links to visit.  For each link a check is
   * performed using {@link #visited visited} to make sure the link
   * has not already been visited.  If it has not, the link is added
   * to <code>visited</code>, and the page is retrieved.  If access
   * to the page has been disallowed by a robots.txt file or a
   * robots META tag, or if there is some other problem retrieving
   * the page, then the page is skipped.  If the page is downloaded
   * successfully {@link #indexPage indexPage} and {@link
   * #getNewLinks getNewLinks} are called if allowed.
   * <code>go</code> terminates when there are no more links to visit
   * or <code>count &gt;= maxCount</code>
   * It also builds a graph to represent the link structure of the pages.
   */
  public void doCrawl() {
    if (linksToVisit.size() == 0) {
      System.err.println("Exiting: No pages to visit.");
      System.exit(0);
    }
    visited = new HashSet<Link>();
    while (linksToVisit.size() > 0 && count < maxCount) {
      // Pause if in slow mode
      if (slow) {
        synchronized (this) {
          try {
            wait(1000);
          }
          catch (InterruptedException e) {
          }
        }
      }
      // Take the top link off the queue
      Link link = linksToVisit.remove(0);
      System.out.println("Trying: " + link);
      // Skip if already visited this page
      if (!visited.add(link)) {
        System.out.println("Already visited");
        continue;
      }
      if (!linkToHTMLPage(link)) {
        System.out.println("Not HTML Page");
        continue;
      }
      HTMLPage currentPage = null;
      Node curNode = null;
      // Use the page retriever to get the page
      try {
        currentPage = retriever.getHTMLPage(link);
      }
      catch (PathDisallowedException e) {
        System.out.println(e);
        continue;
      }
      if (currentPage.empty()) {
        System.out.println("No Page Found");
        continue;
      }
      if (currentPage.indexAllowed()) {
        count++;
        System.out.println("Indexing" + "(" + count + "): " + link);
        indexPage(currentPage);
        curNode = linkGraph.getNode(link.toString());
      }
      if (count < maxCount) {
        List<Link> newLinks = getNewLinks(currentPage);
        addToGraph(curNode, newLinks);
        // System.out.println("Adding the following links" + newLinks);
        // Add new links to end of queue
        linksToVisit.addAll(newLinks);
      }
    }
  }

  /* Add html links to linkGraph */
  private void addToGraph(Node curNode, List<Link> newLinks){
       for (Link tempLink : newLinks) {
           if(linkToHTMLPage(tempLink)){
               Node tempNode = linkGraph.getNode(tempLink.toString());
               linkGraph.addEdge(curNode.toString(), tempNode.toString());
           }  
        } 
  }

   /**
   * Performs the crawl.  Should be called after
   * <code>doCrawl</code> has been called.
   **/
   public void computePageRanks(){
       System.out.println("Graph Structure:");
       linkGraph.print();
   }



  /**
   * Spider the web according to the following command options,
   * but stay within the given site (same URL host).
   * <ul>
   * <li>-safe : Check for and obey robots.txt and robots META tag
   * directives.</li>
   * <li>-d &lt;directory&gt; : Store indexed files in &lt;directory&gt;.</li>
   * <li>-c &lt;count&gt; : Store at most &lt;count&gt; files.</li>
   * <li>-u &lt;url&gt; : Start at &lt;url&gt;.</li>
   * <li>-slow : Pause briefly before getting a page.  This can be
   * useful when debugging.
   * </ul>
   */
  public static void main(String args[]) {
    new PageRankSpider().go(args);
  }

}