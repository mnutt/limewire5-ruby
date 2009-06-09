package org.limewire.ui.swing.search.model;

import java.util.Arrays;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.MockSearchResult;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Provider;

public class SearchResultAdapterTest extends TestCase {
    
    private MockSearchResult result;
    private MockPropertiableHeadings propertiableHeadings;
    private SearchResultAdapter adapter;
    private Provider<PropertiableHeadings> provider;
    private Mockery context;

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() {
        result = new MockSearchResult();
        propertiableHeadings = new MockPropertiableHeadings();
        context = new Mockery();
        provider = context.mock(Provider.class);
        adapter = new SearchResultAdapter(Arrays.asList(new SearchResult[] { result }), provider);
    }

    public void testHeadingAndSubHeadingCached() {
        propertiableHeadings.heading = "foo";
        propertiableHeadings.subheading = "bar";
        
        context.checking(new Expectations(){
            {
                allowing(provider).get();
                will(returnValue(propertiableHeadings));
            }
        });
        
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
        
        context.checking(new Expectations(){
            {
                allowing(provider).get();
                will(returnValue(propertiableHeadings));
            }
        });
        
        assertEquals("foo", adapter.getHeading());
        assertTrue(adapter.isSpam());
        
    }
    
    public void testHeadingWithAnyMarkupIsAutomaticallySanitizedAndMarkedAsSpam() {
        propertiableHeadings.heading = "<html><a href=\"http://www.booya.com/&?foo:#23432-heynow\">foo</a></html>";
        
        context.checking(new Expectations(){
            {
                allowing(provider).get();
                will(returnValue(propertiableHeadings));
            }
        });
        
        assertEquals("foo", adapter.getHeading());
        assertTrue(adapter.isSpam());
    }

    public void testSubHeadingWithHTMLIsAutomaticallySanitizedAndMarkedAsSpam() {
        propertiableHeadings.subheading = "<html><b>foo</b></html>";
        
        context.checking(new Expectations(){
            {
                allowing(provider).get();
                will(returnValue(propertiableHeadings));
            }
        });
        
        assertEquals("foo", adapter.getSubHeading());
        assertTrue(adapter.isSpam());
    }
}
