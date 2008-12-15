package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;

import junit.framework.TestCase;

public class SearchResultTruncatorTest extends TestCase {
    private SearchResultTruncatorImpl trunc;
    private MockFontWidthResolver mockResolver;

    protected void setUp() {
        trunc = new SearchResultTruncatorImpl();
        mockResolver = new MockFontWidthResolver();
    }
    
    public void testFitsWithinWidthDoesNoProcessing() {
        mockResolver.width = 5;
        assertEquals("heynow", trunc.truncateHeading("heynow", 6, mockResolver));
    }
    
    public void testTruncateEndOfText() {
        String result = "<b>Foo</b> is my favorite word";
        String expected = "<b>Foo</b> is my...";
        mockResolver.satisfactoryWidth = 9;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 9, mockResolver));
    }
    
    public void testTruncateBeginningOfText() {
        String result = "My favorite word is <b>Foo</b>";
        String expected = "...rd is <b>Foo</b>";
        mockResolver.satisfactoryWidth = 9;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 9, mockResolver));
    }
    
    public void testTruncateToMiddleOfText() {
        String result = "I work for the <b>department</b> of redundancy <b>department</b>";
        String expected = "... the <b>department</b>...";
        mockResolver.satisfactoryWidth = 15;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 15, mockResolver));
    }
    
    public void testLongTextHasNoBoldMatch() {
        String result = "I work for the department of redundancy department";
        String expected = "I work for the ...";
        mockResolver.satisfactoryWidth = 15;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 15, mockResolver));
    }
    
    public void testStripMultipleSpacesOutOfText() {
        String result = "I work\r    \nfor  \t  the  department   of  redundancy   department";
        String expected = "I work for the ...";
        mockResolver.satisfactoryWidth = 15;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 15, mockResolver));
    }

    public void testStripEmbeddedWhitespaceCharactersOutOfText() {
        String result = "I wo\rk for \the departme\nt of redundancy department";
        String expected = "I wo k for he departme t...";
        mockResolver.satisfactoryWidth = 22;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 22, mockResolver));
    }
    
    public void testStripHTMLCaseInsensitivelyTags() {
        String result = "<html>I work for the department of redundancy department</html>";
        String expected = "I work for the department...";
        mockResolver.satisfactoryWidth = 22;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 22, mockResolver));
        
        result = "<HTML>I work for the department of redundancy department</HTML>";
        assertEquals(expected, trunc.truncateHeading(result, 22, mockResolver));
    }

    public void testCantTruncateSmallTextWhenThePixelWidthIsTooSmall() {
        String result = "Im";
        String expected = "Im";
        mockResolver.satisfactoryWidth = 1;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 1, mockResolver));
    }
    
    public void testTruncateSmallText() {
        String result = "I'm a foo";
        String expected = "I...";
        mockResolver.satisfactoryWidth = 4;
        mockResolver.satisfactoryString = expected;
        assertEquals(expected, trunc.truncateHeading(result, 4, mockResolver));
    }
    
    private static class MockFontWidthResolver implements FontWidthResolver {
        int width;
        int satisfactoryWidth;
        String satisfactoryString;
        @Override
        public int getPixelWidth(String text) {
            if (satisfactoryString != null) {
                if (text.equals(satisfactoryString)) {
                    return satisfactoryWidth;
                }
                return Integer.MAX_VALUE;
            }
            return width;
        }
    }
}
