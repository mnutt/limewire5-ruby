package org.limewire.ui.swing.search.model;

import java.util.Comparator;

import junit.framework.TestCase;

import org.limewire.core.api.FilePropertyKey;

/**
 * Test case for SortFactory. 
 */
public class SortFactoryTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /** Tests method to retrieve sort comparator by option. */
    public void testGetSortComparator() {
        // Get all sort options.
        SortOption[] sortOptions = SortOption.values();
        
        // Verify comparator is available for every sort option.
        for (SortOption sortOption : sortOptions) {
            Comparator<VisualSearchResult> comparator = SortFactory.getSortComparator(sortOption);
            
            assertNotNull("sort comparator", comparator);
        }
    }

    /** Tests method to retrieve date comparator. */ 
    public void testGetDateComparator() {
        // Create test search results.
        MockVisualSearchResult vsr1 = new MockVisualSearchResult("Hello");
        MockVisualSearchResult vsr2 = new MockVisualSearchResult("World");
        
        vsr1.getProperties().put(FilePropertyKey.DATE_CREATED, new Long(2));
        vsr2.getProperties().put(FilePropertyKey.DATE_CREATED, new Long(1));
        
        // Get date comparator.
        Comparator<VisualSearchResult> comparator = 
            SortFactory.getDateComparator(FilePropertyKey.DATE_CREATED, true);
        
        // Verify compare.
        int result = comparator.compare(vsr1, vsr2);
        assertTrue("date comparator", (result > 0));
    }

    /** Tests method to retrieve long comparator. */ 
    public void testGetLongComparator() {
        // Create test search results.
        MockVisualSearchResult vsr1 = new MockVisualSearchResult("Hello");
        MockVisualSearchResult vsr2 = new MockVisualSearchResult("World");
        
        vsr1.getProperties().put(FilePropertyKey.YEAR, 2009L);
        vsr2.getProperties().put(FilePropertyKey.YEAR, 2008L);
        
        // Get long comparator.
        Comparator<VisualSearchResult> comparator = 
            SortFactory.getLongComparator(FilePropertyKey.YEAR, true);
        
        // Verify compare.
        int result = comparator.compare(vsr1, vsr2);
        assertTrue("long comparator", (result > 0));
    }

    /** Tests method to retrieve name comparator. */ 
    public void testGetNameComparator() {
        // Create test search results.
        MockVisualSearchResult vsr1 = new MockVisualSearchResult("Hello");
        MockVisualSearchResult vsr2 = new MockVisualSearchResult("World");
        
        // Get name comparator.
        Comparator<VisualSearchResult> comparator = SortFactory.getNameComparator(true);
        
        // Verify compare.
        int result = comparator.compare(vsr1, vsr2);
        assertTrue("name comparator", (result < 0));
    }

    /** Tests method to retrieve relevance comparator. */
    public void testGetRelevanceComparator() {
        // Get relevance comparator.
        Comparator<VisualSearchResult> comparator = SortFactory.getRelevanceComparator();
        
        // Create test search results.
        MockVisualSearchResult vsr1 = new MockVisualSearchResult("Hello");
        MockVisualSearchResult vsr2 = new MockVisualSearchResult("World");
        
        // Verify compare for equal relevance - name values are compared.
        int result = comparator.compare(vsr1, vsr2);
        assertTrue("equal relevance", (result < 0));

        // Set test relevance values.
        vsr1.setRelevance(1);
        vsr2.setRelevance(2);
        
        // Verify compare for non-equal relevance - order is descending.
        result = comparator.compare(vsr1, vsr2);
        assertTrue("non-equal relevance", (result > 0));
    }

    /** Tests method to retrieve relevance comparator with sort order. */ 
    public void testGetRelevanceComparatorBoolean() {
        // Get relevance comparator.
        Comparator<VisualSearchResult> comparator = SortFactory.getRelevanceComparator(true);
        
        // Create test search results.
        MockVisualSearchResult vsr1 = new MockVisualSearchResult("Hello");
        MockVisualSearchResult vsr2 = new MockVisualSearchResult("World");
        
        // Verify compare for equal relevance.
        int result = comparator.compare(vsr1, vsr2);
        assertTrue("equal relevance", (result == 0));

        // Set test relevance values.
        vsr1.setRelevance(1);
        vsr2.setRelevance(2);
        
        // Verify compare for non-equal relevance.
        result = comparator.compare(vsr1, vsr2);
        assertTrue("non-equal relevance", (result < 0));
    }

    /** Tests method to retrieve string comparator. */ 
    public void testGetStringComparator() {
        // Create test search results.
        MockVisualSearchResult vsr1 = new MockVisualSearchResult("Hello");
        MockVisualSearchResult vsr2 = new MockVisualSearchResult("World");
        
        vsr1.getProperties().put(FilePropertyKey.TITLE, "zulu");
        vsr2.getProperties().put(FilePropertyKey.TITLE, "yankee");
        
        // Get string comparator.
        Comparator<VisualSearchResult> comparator = 
            SortFactory.getStringComparator(FilePropertyKey.TITLE, true);
        
        // Verify compare.
        int result = comparator.compare(vsr1, vsr2);
        assertTrue("string comparator", (result > 0));
    }

    /** 
     * Tests method to retrieve string comparator.  Should be case-insensitive.
     */ 
    public void testGetStringComparatorMixedCase() {
        // Create test search results.
        MockVisualSearchResult vsr1 = new MockVisualSearchResult("Hello");
        MockVisualSearchResult vsr2 = new MockVisualSearchResult("World");
        
        vsr1.getProperties().put(FilePropertyKey.TITLE, "Zulu");
        vsr2.getProperties().put(FilePropertyKey.TITLE, "yankee");
        
        // Get string comparator.
        Comparator<VisualSearchResult> comparator = 
            SortFactory.getStringComparator(FilePropertyKey.TITLE, true);
        
        // Verify compare is case-insensitive.
        int result = comparator.compare(vsr1, vsr2);
        assertTrue("string comparator", (result > 0));
    }
}
