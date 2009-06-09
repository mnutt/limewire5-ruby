package org.limewire.ui.swing.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.filter.FilterableItemTextFilterator;
import org.limewire.util.BaseTestCase;

/**
 * Test case for FilterableItemTextFilterator. 
 */
public class FilterableItemTextFilteratorTest extends BaseTestCase {
    /** Instance of class to be tested. */
    private FilterableItemTextFilterator<MockFilterableItem> filterator;

    /**
     * Constructs a test case for the specified method name.
     */
    public FilterableItemTextFilteratorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filterator = new FilterableItemTextFilterator<MockFilterableItem>();
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
        
        // Create test item, and add indexable properties.
        MockFilterableItem item = new MockFilterableItem("Test");
        
        Map<FilePropertyKey, Object> propertyMap = item.getProperties();
        propertyMap.put(FilePropertyKey.TITLE, PROPERTY_1);
        propertyMap.put(FilePropertyKey.AUTHOR, PROPERTY_2);
        
        // Get filter strings from filterator.
        List<String> stringList = new ArrayList<String>();
        filterator.getFilterStrings(stringList, item);
        
        // Verify indexable properties are in list.
        boolean found1 = false;
        boolean found2 = false;
        for (String value : stringList) {
            if (PROPERTY_1.equals(value)) {
                found1 = true;
            } else if (PROPERTY_2.equals(value)) {
                found2 = true;
            }
        }
        
        assertTrue("filter string 1", found1);
        assertTrue("filter string 2", found2);
    }
}
