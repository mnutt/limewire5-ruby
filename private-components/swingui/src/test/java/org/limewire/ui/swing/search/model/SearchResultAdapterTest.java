package org.limewire.ui.swing.search.model;

import java.util.Arrays;

import junit.framework.TestCase;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.MockSearchResult;

public class SearchResultAdapterTest extends TestCase {
    
    private MockSearchResult result;
    private MockPropertiableHeadings propertiableHeadings;
    private SearchResultAdapter adapter;

    protected void setUp() {
        result = new MockSearchResult();
        propertiableHeadings = new MockPropertiableHeadings();
        adapter = new SearchResultAdapter(Arrays.asList(new SearchResult[] { result }), propertiableHeadings);
    }

    public void testHeadingAndSubHeadingCached() {
        propertiableHeadings.heading = "foo";
        propertiableHeadings.subheading = "bar";
        
        assertEquals("foo", adapter.getHeading());
        assertEquals("bar", adapter.getSubHeading());
        assertEquals(1, propertiableHeadings.getHeadingCalledCount);
        assertEquals(1, propertiableHeadings.getSubHeadingCalledCount);
        //Call again to test caching
        adapter.getHeading();
        adapter.getSubHeading();
        assertEquals(1, propertiableHeadings.getHeadingCalledCount);
        assertEquals(1, propertiableHeadings.getSubHeadingCalledCount);
    }
    
    public void testHeadingWithHTMLIsAutomaticallySanitizedAndMarkedAsSpam() {
        propertiableHeadings.heading = "<html><b>foo</b></html>";
        
        assertEquals("foo", adapter.getHeading());
        assertTrue(adapter.isSpam());
        
    }
    
    public void testHeadingWithAnyMarkupIsAutomaticallySanitizedAndMarkedAsSpam() {
        propertiableHeadings.heading = "<html><a href=\"http://www.booya.com/&?foo:#23432-heynow\">foo</a></html>";
        
        assertEquals("foo", adapter.getHeading());
        assertTrue(adapter.isSpam());
    }

    public void testSubHeadingWithHTMLIsAutomaticallySanitizedAndMarkedAsSpam() {
        propertiableHeadings.subheading = "<html><b>foo</b></html>";
        
        assertEquals("foo", adapter.getSubHeading());
        assertTrue(adapter.isSpam());
    }
}
