package org.limewire.ui.swing.search.resultpanel.list;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

public class SearchHighlightUtil {
    private static final Pattern FIND_SPACES = Pattern.compile(" ");
    private static final Log LOG = LogFactory.getLog(SearchHighlightUtil.class);

    private SearchHighlightUtil() {
    }

    /**
     * Returns a string that highlights the specified search text within the
     * specified content.  Highlights are indicated using HTML &lt;b&gt; tags.
     * Multiple search words are separated by space characters; matches occur
     * on word boundaries.
     */
    public static String highlight(String search, String content) {
        if (content == null) {
            return "";
        }
        
        if (search == null) {
            return content;
        }
        
        StringBuilder bldr = new StringBuilder();
        int index = 0;
        
        // Replace space characters with logical OR operator.
        String scrubbedSearch = FIND_SPACES.matcher(search).replaceAll("|");

        // Create literal string with escaped characters.
        String literalSearch = escapeLiteralChars(scrubbedSearch);
        
        // Create pattern to match on word boundary, and match content.
        Pattern pattern = Pattern.compile("\\b(" + literalSearch + ")", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        
        index = 0;
        while (matcher.find()) {
            MatchResult result = matcher.toMatchResult();

            int startIndex = result.start();
            bldr.append(content.substring(index, startIndex));
            String word = result.group();
            bldr.append("<b>").append(word).append("</b>");
            index = matcher.end();

            LOG.debugf("Start: {0} url: {1} end: {2}", startIndex, word, matcher.end());
        }

        if (bldr.length() > 0) {
            bldr.append(content.substring(index));
            return bldr.toString();
        }
        return content;
    }
    
    /**
     * Returns a literal String for the specified String by inserting a 
     * backslash ('\') to escape special characters.  This method is a 
     * substitute for <code>Matcher.quoteReplacement(String)</code>, which only 
     * escapes backslash and dollar sign ('$') characters.
     */
    private static String escapeLiteralChars(String s) {
        // Define characters that require escape.  Note that many regex
        // characters are changed into literals because real-world users would
        // not expect regex behavior.
        final String SPECIAL_CHARS = "[\\$*()";
        
        // Process all characters, and insert escape when needed.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }

        // Return result
        return sb.toString();
    }
}
