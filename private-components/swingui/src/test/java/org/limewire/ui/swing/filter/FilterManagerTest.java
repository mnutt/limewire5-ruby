package org.limewire.ui.swing.filter;

import java.util.List;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.filter.Filter;
import org.limewire.ui.swing.filter.FilterManager;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for FilterManager.
 */
public class FilterManagerTest extends BaseTestCase {
    /** Instance of class to be tested. */
    private FilterManager<MockFilterableItem> filterManager;

    /**
     * Constructs a test case for the specified method name.
     */
    public FilterManagerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filterManager = new FilterManager<MockFilterableItem>(
                new MockFilterableSource(SearchCategory.ALL), null);
    }

    @Override
    protected void tearDown() throws Exception {
        filterManager = null;
        super.tearDown();
    }

    /** Tests method to retrieve category filter. */
    public void testGetCategoryFilter() {
        assertNotNull("category filter", filterManager.getCategoryFilter());
    }

    /** Tests method to retrieve file source filter. */
    public void testGetSourceFilter() {
        assertNotNull("source filter", filterManager.getSourceFilter());
    }

    /** Tests method to retrieve minimum property filters to display. */
    public void testGetPropertyFilterMinimum() {
        // Verify values for all categories.
        for (SearchCategory category : SearchCategory.values()) {
            int minimum = filterManager.getPropertyFilterMinimum(category);
            assertTrue("property filter min", (minimum == -1) || (minimum > 0));
        }
    }

    /** Tests method to retrieve list of property filters. */
    public void testGetPropertyFilterList() {
        // Verify lists for all categories.
        for (SearchCategory category : SearchCategory.values()) {
            List<Filter<MockFilterableItem>> filterList = filterManager.getPropertyFilterList(category);
            assertTrue("property filter min", (filterList.size() > 0));
        }
    }
}
