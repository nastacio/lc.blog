/********************************************************** {COPYRIGHT-TOP} ****
 * IBM Confidential
 * OCO Source Materials
 *
 * (C) Copyright IBM Corp. 2013  All Rights Reserved.
 *
 * The source code for this program is not published or otherwise  
 * divested of its trade secrets, irrespective of what has been 
 * deposited with the U.S. Copyright Office.
 ********************************************************* {COPYRIGHT-END} ****/

package com.ibm.jazzsm.lc;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.parser.Parser;

/**
 * Scans a Lotus Connections Blog atom feed and pulls basic statistics, such as
 * title, author, publishing dates, and hits.
 * 
 * @author Denilson Nastacio
 * 
 */
public class HitCounter {

	/**
	 * Atom element for the overall rank element of an entry , which includes hit counters.
	 */
    private static final QName RANK_ATTR = new QName("http://www.ibm.com/xmlns/prod/sn", "rank");
    
    /**
	 * Atom attribute for the hit counter within a rank element.
     */
    private static final String RANK_HITS = "http://www.ibm.com/xmlns/prod/sn/hit";
    
    /**
     * Default ATOM feed when not specified elsewhere. 
     */
    private static final String DEFAULT_ATOM_FEED = "https://www.ibm.com/developerworks/community/blogs/roller-ui/rendering/feed/69ec672c-dd6b-443d-add8-bb9a9a490eba/entries/atom?lang=en";

    /*
     * Public methods.
     */

    /**
     * Parses an input feed and dumps basic statistics to a file or standard
     * output
     * 
     * @param args
     *            args[0] is the name of the output file. If not specified,
     *            output goes to standard output
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String mainLink = DEFAULT_ATOM_FEED;
        String outFile = null;
        if (args.length > 0) {
            outFile = args[0];
        }
        PrintStream out = null == outFile ? System.out : new PrintStream(outFile);

        out.println("Title|Author|URL|Date|Date(long)|hits");

        Parser abderaParser = Abdera.getNewParser();

        String urlStr = mainLink;

        while (urlStr != null) {
            URL inputUrl = new URL(urlStr);
            URLConnection conn = inputUrl.openConnection();
            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.11) Gecko/20101012 (CK-IBM) Firefox/3.6.11 ( .NET CLR 3.5.30729)");
            InputStream inFeed = conn.getInputStream();
            try {
                Document<Feed> doc = abderaParser.parse(inFeed);
                Feed feed = doc.getRoot();
                List<Entry> feedEntries = feed.getEntries();
                for (Entry entry : feedEntries) {
                    out.print(entry.getTitle());
                    out.print("|");
                    out.print(entry.getAuthor().getName().toString());
                    out.print("|");
                    out.print(entry.getLink("alternate").getHref().toASCIIString());
                    out.print("|");
                    out.print(entry.getUpdated().toString());
                    out.print("|");
                    out.print(entry.getUpdated().getTime());
                    out.print("|");
                    List<Element> extensions = entry.getExtensions(RANK_ATTR);
                    boolean noHitCount = true;
                    for (Element element : extensions) {
                        String scheme = element.getAttributeValue("scheme");
                        if (RANK_HITS.equals(scheme)) {
                            noHitCount = false;
                            String hitsStr = element.getText();
                            long hits = Long.parseLong(hitsStr);
                            out.println(hits);
                        }
                    }
                    if (noHitCount) {
                        out.println();
                    }
                }
                Link nextLink = feed.getLink("next");
                urlStr = null == nextLink ? null : nextLink.getHref().toASCIIString();

            } finally {
                inFeed.close();
            }
        }

        if (outFile != null) {
            out.close();
        }
    }
}
