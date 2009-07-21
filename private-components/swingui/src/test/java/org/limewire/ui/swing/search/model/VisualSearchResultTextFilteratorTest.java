package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;

import junit.framework.TestCase;

/**
 * Test case for VisualSearchResultTextFilterator. 
 */
public class VisualSearchResultTextFilteratorTest extends TestCase {
    /** Instance of class to be tested. */
    private VisualSearchResultTextFilterator filterator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filterator = new VisualSearchResultTextFilterator();
    }

    @Override
    protected void tearDown() throws Exception {
        filterator = null;
        super.tearDown();
    }

    /** Tests method to retrieve filter strings. */
    public void testGetFilterStrings() {
        // Define test values.
        final String PROPERTY_1 = "Title";
        final String PROPERTY_2 = "Author";
        
        // Create test search result, and add indexable properties.
        MockVisualSearchResult vsr = new MockVisualSearchResult("Test");
        
        Map<FilePropertyKey, Object> propertyMap = vsr.getProperties();
        propertyMap.put(FilePropertyKey.TITLE, PROPERTY_1);
        propertyMap.put(FilePropertyKey.AUTHOR, PROPERTY_2);
        
        // Get filter strings from filterator.
        List<String> stringList = new ArrayList<String>();
        filterator.getFilterStrings(stringList, vsr);
        
        // Verify indexable properties are in list.
        boolean found1 = false;
        boolean found2 = false;
        for (String item : stringList) {
            if (PROPERTY_1.equals(item)) {
                found1 = true;
            } else if (PROPERTY_2.equals(item)) {
                found2 = true;
            }
        }
        
        assertTrue("filter string 1", found1);
        assertTrue("filter string 2", found2);
    }
}
