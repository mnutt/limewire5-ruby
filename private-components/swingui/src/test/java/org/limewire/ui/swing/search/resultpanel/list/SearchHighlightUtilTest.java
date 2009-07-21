package org.limewire.ui.swing.search.resultpanel.list;

import junit.framework.TestCase;

public class SearchHighlightUtilTest extends TestCase {
    
    private String highlight(String search, String current) {
        SearchHighlightUtil util = new SearchHighlightUtil(search);
        return util.highlight(current);
    }
    
    public void testNoMatches() {
        assertEquals("heynow", highlight("foo bar", "heynow"));
    }
    
    public void testNullInputString() {
        assertEquals("", highlight("foo", null));
    }
    
    public void testMatchOneWordSearch() {
        assertEquals("i like <b>foo</b>d", highlight("foo", "i like food"));
    }
    
    public void testMatchTwoWordSearch() {
        assertEquals("i like the <b>foo</b>d <b>bar</b> in the mall", 
                highlight("foo bar", "i like the food bar in the mall"));
    }

    public void testMatchCaseInsensitively() {
        assertEquals("i like the <b>foo</b>d <b>bar</b> in the mall", 
                highlight("FoO bAr", "i like the food bar in the mall"));
    }
    
    public void testNoHighlightingIfSearchTermsNotAtBeginningOfWord() {
        assertEquals("i like the pfood sbar in the mall", 
                highlight("foo bar", "i like the pfood sbar in the mall"));
    }
    
    public void testMultipleSearchMatchesAndOneExclusion() {
        assertEquals("<b>foo</b> <b>foo</b>ey ooff <b>hoo</b>faa", 
                highlight("foo faa hoo", "foo fooey ooff hoofaa"));
    }
    
    /** Tests highlighting with open bracket. */
    public void testHighlightWithOpenBracket() {
        // Define test strings.
        String search = "Akon[";
        String content = "Akon[hello]";
        
        // Verify highlighting.
        String expectedReturn = "<b>Akon[</b>hello]";
        String actualReturn = highlight(search, content);
        assertEquals("highlight with open bracket", expectedReturn, actualReturn);
    }
    
    /** Tests highlighting with asterisk character. */
    public void testHighlightWithAsterisk() {
        // Define test strings.
        String search = "f*";
        String content = "f*[x]";
        
        // Verify highlighting.
        String expectedReturn = "<b>f*</b>[x]";
        String actualReturn = highlight(search, content);
        assertEquals("highlight with asterisk", expectedReturn, actualReturn);
    }
    
    /** Tests highlighting with period character. */
    public void testHighlightWithPeriod() {
        // Define test strings.
        String search = "f.";
        String content = "f.bar[x]";

        // Verify highlighting.
        String expectedReturn = "<b>f.</b>bar[x]";
        String actualReturn = highlight(search, content);
        assertEquals("highlight with period", expectedReturn, actualReturn);

        // Verify non-match.
        String moreContent = "fubar[x]";
        
        expectedReturn = "fubar[x]";
        actualReturn = highlight(search, moreContent);
        assertEquals("highlight with period", expectedReturn, actualReturn);
    }
}
