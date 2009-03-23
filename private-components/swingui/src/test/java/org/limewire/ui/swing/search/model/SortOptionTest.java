package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchCategory;

import junit.framework.TestCase;

/**
 * Test case for SortOption. 
 */
public class SortOptionTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /** Tests method to retrieve default option. */
    public void testGetDefault() {
        // Verify default option is available.
        assertNotNull("default option", SortOption.getDefault());
    }

    /** Tests method to retrieve sort option by search category. */
    public void testGetSortOptions() {
        // Verify sort options are available for every search category.
        for (SearchCategory category : SearchCategory.values()) {
            SortOption[] options = SortOption.getSortOptions(category);
            
            assertTrue("category options", ((options != null) && (options.length > 0)));
        }
    }
}
