package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.list.SearchHighlightUtil.highlight;
import junit.framework.TestCase;

public class SearchHighlightUtilTest extends TestCase {
    
    public void testNoMatches() {
        assertEquals("heynow", highlight("foo bar", "heynow"));
    }
    
    public void testNullInputString() {
        assertEquals("", highlight("foo", null));
    }
    
    public void testNullSearchString() {
        assertEquals("howdy", highlight(null, "howdy"));
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
}
