package com.limegroup.gnutella.gui.search;

import javax.swing.text.BadLocationException;

import junit.framework.Test;

import org.limewire.core.settings.SearchSettings;
import org.limewire.util.I18NConvert;

import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.search.SearchField.SearchFieldDocument;
import com.limegroup.gnutella.LimeTestUtils;

public class SearchFieldTest extends GUIBaseTestCase {

    public SearchFieldTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(SearchFieldTest.class); 
    }
    
    /**
     * Tests if too long documents can be created. 
     * @throws BadLocationException 
     */
    public void testSearchFieldDocumentInsertString() throws BadLocationException {
        SearchFieldDocument doc = new SearchFieldDocument();
        int maxQueryLength = SearchSettings.MAX_QUERY_LENGTH.getValue();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxQueryLength; i++) {
            builder.append('a');
        }
        String s = builder.toString();
        doc.insertString(0, s, null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing if insertion at end fails
        doc.insertString(s.length(), "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing if insertion at beginning fails
        doc.insertString(0, "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing if insertion in the middle fails
        doc.insertString(doc.getLength() / 2, "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        // testing for length - 1
        doc.insertString(doc.getLength() - 1, "b", null);
        assertEquals(s, doc.getText(0, doc.getLength()));
        
        doc = new SearchFieldDocument();
        // too long string by one
        s += "a";
        doc.insertString(0, s, null);
        assertEquals(0, doc.getLength());
        
        // emulate user input
        s = "hello world foo bar";
        assertTrue(s.length() < maxQueryLength);
        for (char c : s.toCharArray()) {
            doc.insertString(doc.getLength(), Character.toString(c), null);
        }
        assertEquals(s, doc.getText(0, doc.getLength()));
    }
    
    public void testSearchFieldDocumentInsertIllegalCharacters() throws Exception {
        SearchFieldDocument doc = new SearchFieldDocument();
        char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
        for (char c : illegalChars) {
            doc.insertString(0, Character.toString(c), null);
            assertEquals(0, doc.getLength());
        }
        for (int i = 0; i < illegalChars.length; i++) {
            // test with legal characters appended or prepended
            String s = i % 2 == 0 ? "a" + illegalChars[i] : illegalChars[i] + "a";
            doc.remove(0, doc.getLength());
            doc.insertString(0, s, null);
            assertEquals("String a of" + s + " was not inserted, " + illegalChars[i], 1, doc.getLength());    
        }
    }
    
    public void testInsertTooLongStringWithCharactersThatAreRemovedInNormalization() throws Exception {
        SearchFieldDocument doc = new SearchFieldDocument();
        int maxLength = SearchSettings.MAX_QUERY_LENGTH.getValue();

        // + and = are removed in normalization and thus a too long query string could slip in
        String toInsertPart1 = "hello === ==++ world that is way too long long long long +==++";
        String toInsertPart2 = LimeTestUtils.generateRepeatingStringByLength("===+++==+=++",
                maxLength-toInsertPart1.length()+1);
        String toInsert = toInsertPart1 + toInsertPart2;
        

        doc.insertString(0, toInsert, null);
        assertEquals(0, doc.getLength());
    }
    
    public void testInsertTooLongStringWithCharactersThatAreAddedByNormalization() throws Exception {
        // preconditons of test
        assertEquals(1, "\uFB01".length());
        assertGreaterThan("\uFB01".length(), I18NConvert.instance().getNorm("\uFB01").length());
        SearchFieldDocument doc = new SearchFieldDocument();
        
        // add incrementally the character that expands to two with normalization
        for (int i = 0; i < SearchSettings.MAX_QUERY_LENGTH.getValue() - 2; i++) {
            doc.insertString(doc.getLength(), "\uFB01", null);
        }
        assertLessThanOrEquals(SearchSettings.MAX_QUERY_LENGTH.getValue(), I18NConvert.instance().getNorm(doc.getText(0, doc.getLength())).length());
        assertLessThanOrEquals(SearchSettings.MAX_QUERY_LENGTH.getValue(), doc.getText(0, doc.getLength()).length());
    }
}
