package org.limewire.ui.swing.util;

import java.util.Comparator;

import ca.odell.glazedlists.gui.TableFormat;
import junit.framework.TestCase;

/**
 * JUnit test case for EventListColumnComparator.
 */
public class EventListColumnComparatorTest extends TestCase {
    /** Instance of class to be tested. */
    private Comparator<TestItem> columnComparator;
    
    private TableFormat<TestItem> tableFormat;
    private Comparator comparator;

    /**
     * Sets up the fixture.  Called before a test is executed.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tableFormat = new TestTableFormat();
        comparator = new TestComparator();
        columnComparator = new EventListColumnComparator<TestItem>(tableFormat, 0, comparator);
    }

    /**
     * Tears down the fixture.  Called after a test is executed.
     */
    @Override
    protected void tearDown() throws Exception {
        columnComparator = null;
        tableFormat = null;
        comparator = null;
        super.tearDown();
    }

    /** Tests method to compare two values of the same type. */
    public void testCompareSameType() {
        TestItem item1 = new TestItem("alpha");
        TestItem item2 = new TestItem("bravo");
        
        // Compare items containing values of same type.
        int expectedReturn = -1;
        int actualReturn = columnComparator.compare(item1, item2);
        assertEquals("compare same type", expectedReturn, actualReturn);
    }


    /** Tests method to compare two values of different type. */
    public void testCompareDifferentType() {
        TestItem item1 = new TestItem(new Long(1));
        TestItem item2 = new TestItem(new String("bravo"));

        Exception ex = null;

        // Compare items containing values of different type.
        try {
            columnComparator.compare(item1, item2);
        } catch (IllegalStateException ise) {
            ex = ise;
        }

        // Verify exception and cause are not null.
        assertNotNull("compare different type", ex);
        if (ex != null) {
            assertNotNull("compare different type", ex.getCause());
        }
    }
    
    /**
     * Test list item that contains a value of any type. 
     */
    private static class TestItem {
        private final Object value;
        
        public TestItem(Object value) {
            this.value = value;
        }
        
        public Object getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value.toString();
        }
    }
    
    /**
     * Test table format.
     */
    private static class TestTableFormat implements TableFormat<TestItem> {
        
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return "Value";
            default:
                return null;
            }
        }

        @Override
        public Object getColumnValue(TestItem baseObject, int column) {
            switch (column) {
            case 0:
                return baseObject.getValue();
            default:
                return null;
            }
        }
    }

    /**
     * Test comparator for Object values.  The compare() method throws an
     * exception if the values are not of the same type.
     */
    private static class TestComparator implements Comparator<Object> {
        
        @Override
        public int compare(Object obj1, Object obj2) {
            if (obj1.getClass().isInstance(obj2)) {
                return String.valueOf(obj1).compareToIgnoreCase(String.valueOf(obj2));
            } else {
                throw new IllegalStateException("Cannot compare " + 
                        obj1.getClass().getName() + " to " + obj2.getClass().getName());
            }
        }
    }
}
