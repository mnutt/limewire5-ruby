package org.limewire.ui.swing.filter;

import java.util.List;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for FileSizeFilterFormat.
 */
public class FileSizeFilterFormatTest extends BaseTestCase {
    /** Instance of class to be tested. */
    private FileSizeFilterFormat<MockFilterableItem> filterFormat;

    /**
     * Constructs a test case for the specified method name.
     */
    public FileSizeFilterFormatTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filterFormat = new FileSizeFilterFormat<MockFilterableItem>();
    }

    @Override
    protected void tearDown() throws Exception {
        filterFormat = null;
        super.tearDown();
    }

    /** Tests method to update values by category. */
    public void testUpdateValues() {
        // Get default values.
        long[] values = filterFormat.getValues();
        
        // Get old values.
        long lowerValue = values[1];
        long upperValue = values[values.length - 2];
        
        // Update filter values.
        filterFormat.updateValues(SearchCategory.VIDEO, lowerValue, upperValue);
        long[] newValues = filterFormat.getValues();
        
        // Verify new values include old values.
        boolean foundLower = false;
        boolean foundUpper = false;
        for (int i = 0; i < newValues.length; i++) {
            if (newValues[i] == lowerValue) foundLower = true;
            if (newValues[i] == upperValue) foundUpper = true;
        }
        
        assertTrue("found old lower value", foundLower);
        assertTrue("found old upper value", foundUpper);
    }

    /** Tests method to create value list. */
    public void testCreateValueList() {
        // Define test array and values in array.
        long[] TEST_VALUES = {0, 10, 20, 30, 40, 50};
        long lowerValue = 10;
        long upperValue = 40;
        
        // Create value list.
        List<Long> valueList = filterFormat.createValueList(TEST_VALUES, lowerValue, upperValue);
        
        // Verify list matches test array.
        assertEquals("list length", TEST_VALUES.length, valueList.size());
        for (int i = 0, len = valueList.size(); i < len; i++) {
            assertEquals("list value", TEST_VALUES[i], valueList.get(i).longValue());
        }
        
        // Define values not in array.
        lowerValue = 15;
        upperValue = 45;
        long[] RESULT_VALUES = {0, 10, 15, 20, 30, 40, 45, 50};
        
        // Create 2nd value list.
        valueList = filterFormat.createValueList(TEST_VALUES, lowerValue, upperValue);
        
        // Verify list includes values.
        assertEquals("list length", RESULT_VALUES.length, valueList.size());
        for (int i = 0, len = valueList.size(); i < len; i++) {
            assertEquals("list value", RESULT_VALUES[i], valueList.get(i).longValue());
        }
    }
}
