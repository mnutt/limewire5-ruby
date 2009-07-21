package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.GUID;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.TextMatcherEditor;

import com.google.inject.Provider;

/**
 * Test case for BasicSearchResultsModel. 
 */
public class BasicSearchResultsModelTest extends BaseTestCase {
    /** Instance of class being tested. */
    private BasicSearchResultsModel model;
    private Provider<PropertiableHeadings> provider;
    private Mockery context;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public BasicSearchResultsModelTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new Mockery();
        provider = context.mock(Provider.class);
        // Create test instance.
        model = new BasicSearchResultsModel(new TestSearchInfo(), 
                new TestSearch(), provider, null, null);
    }
    
    @Override
    protected void tearDown() throws Exception {
        model = null;
        super.tearDown();
    }
    
    private void waitForUiThread() {
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
    }
    
    private void addResult(BasicSearchResultsModel model, SearchResult result) {
        model.addSearchResult(result);
        waitForUiThread();
    }

    public void testGroupingByName2UrnsNameComesEarly() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("1", "other file");
        // other file for urn1 is coming in early
        TestSearchResult testResult3 = new TestSearchResult("2", "other file");
        TestSearchResult testResult4 = new TestSearchResult("1", "file name");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(3, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }

    public void testGroupingByName2UrnsNameComesLate() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("1", "file name");
        // other file for urn1 is coming in late
        TestSearchResult testResult4 = new TestSearchResult("1", "other file");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(group0.toString(), 0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(3, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(1, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group1.getSimilarityParent());
        assertEquals(group1, group0.getSimilarityParent());

    }

    public void testGroupingByName2UrnsNameComesLateMultipleAdds() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("1", "file name");
        TestSearchResult testResult4 = new TestSearchResult("1", "other file");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(9, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(1, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(3, coreResults1.size());

        assertNull(group1.getSimilarityParent());
        assertEquals(group1, group0.getSimilarityParent());

    }

    public void testGroupByName4Urns() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "other file");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("3", "other file");
        TestSearchResult testResult4 = new TestSearchResult("4", "other file");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(4, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(3, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(1, coreResults2.size());

        VisualSearchResult group3 = results.get(3);
        List<VisualSearchResult> similarResults3 = group3.getSimilarResults();
        assertEquals(0, similarResults3.size());
        List<SearchResult> coreResults3 = group1.getCoreSearchResults();
        assertEquals(1, coreResults3.size());

        assertEquals(group0, group1.getSimilarityParent());
        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group2.getSimilarityParent());
        assertEquals(group0, group3.getSimilarityParent());

    }

    public void testGroupingByName3Urns() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "other file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "other file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "other file");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        addResult(model, testResult5);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(2, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(1, coreResults2.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());
        assertEquals(group0, group2.getSimilarityParent());

    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        addResult(model, testResult5);
        addResult(model, testResult6);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(2, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(2, coreResults2.size());

        assertNull(group1.getSimilarityParent());
        assertEquals(group1, group0.getSimilarityParent());
        assertEquals(group1, group2.getSimilarityParent());

    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty3GroupHasMoreFiles() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");
        TestSearchResult testResult7 = new TestSearchResult("3", "blah3 file");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        addResult(model, testResult5);
        addResult(model, testResult6);
        addResult(model, testResult7);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(2, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(3, coreResults2.size());

        assertNull(group1.getSimilarityParent());
        assertEquals(group1, group2.getSimilarityParent());
        assertEquals(group1, group0.getSimilarityParent());

    }

    public void testVisibility() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        
        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah1 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");

        
        addResult(model, testResult1);
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(1, results.size());
        VisualSearchResult result0 = results.get(0);
        assertTrue(result0.isVisible());
        assertFalse(result0.isChildrenVisible());
        result0.setChildrenVisible(true);
        assertTrue(result0.isVisible());
        assertTrue(result0.isChildrenVisible());

        addResult(model, testResult2);
        assertTrue(result0.isVisible());
        assertTrue(result0.isChildrenVisible());

        addResult(model, testResult3);
        assertTrue(result0.isVisible());
        assertTrue(result0.isChildrenVisible());
        List<VisualSearchResult> children = result0.getSimilarResults();
        assertEquals(1, children.size());
        VisualSearchResult child = children.get(0);
        assertTrue(child.isVisible());

        result0.setChildrenVisible(false);
        assertTrue(result0.isVisible());
        assertFalse(result0.isChildrenVisible());
        assertFalse(child.isVisible());
        result0.setChildrenVisible(true);

        addResult(model, testResult4);
        addResult(model, testResult5);
        children = result0.getSimilarResults();
        assertEquals(2, children.size());
        VisualSearchResult child0 = children.get(0);
        VisualSearchResult child1 = children.get(1);
        assertTrue(child0.isVisible());
        assertTrue(child1.isVisible());
        result0.setChildrenVisible(false);

        addResult(model, testResult6);
        children = result0.getSimilarResults();
        assertEquals(2, children.size());
        child0 = children.get(0);
        child1 = children.get(1);
        assertFalse(child0.isVisible());
        assertFalse(child1.isVisible());
        result0.setChildrenVisible(true);
        assertTrue(child0.isVisible());
        assertTrue(child1.isVisible());

        results = model.getUnfilteredList();
        assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(2, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(2, coreResults2.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group2.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }
    
    public void testSameNameHyphenNameHyphenName() {

        
        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test-foo-bar");
        SearchResult searchResult1 = new TestSearchResult("1", "test-foo-bar.mp3", properties1); 
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "test-foo-bar");
        SearchResult searchResult2 = new TestSearchResult("2", "test-foo-bar.mp3", properties2);
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }
    
    public void testNotSameNameOrButSameTrackMetaData() {

        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test");
        SearchResult searchResult1 = new TestSearchResult("1", "test.mp3", properties1);
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "blah123");
        properties2.put(FilePropertyKey.TITLE, "test");
        SearchResult searchResult2 = new TestSearchResult("2", "blah123.mp3", properties2);
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        //should be no similar results
        assertNull(group1.getSimilarityParent());
        assertNull(group0.getSimilarityParent());


    }
    
    public void testSameNameOrAlbumAndTrackMetaData() {

        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test-blah");
        SearchResult searchResult1 = new TestSearchResult("1", "test-blah.mp3", properties1);
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "blah123");
        properties2.put(FilePropertyKey.ALBUM, "test");
        properties2.put(FilePropertyKey.TITLE, "blah");
        SearchResult searchResult2 = new TestSearchResult("2", "blah123.mp3", properties2);
        
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }
    
    public void testSameNameOrArtistAndTrackMetaData() {

        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test-blah");
        SearchResult searchResult1 = new TestSearchResult("1", "test-blah.mp3", properties1);
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "blah123");
        properties2.put(FilePropertyKey.AUTHOR, "test");
        properties2.put(FilePropertyKey.TITLE, "blah");
        SearchResult searchResult2 = new TestSearchResult("2", "blah123.mp3", properties2);
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }

    /** Tests method to retrieve filtered search results. */
    public void testGetFilteredSearchResults() {

        // Create test search results.
        TestSearchResult testResult1 = new TestSearchResult("1", "xray");
        TestSearchResult testResult2 = new TestSearchResult("2", "zulu");
        TestSearchResult testResult3 = new TestSearchResult("3", "whiskey");
        TestSearchResult testResult4 = new TestSearchResult("4", "yankee");
        testResult3.setCategory(Category.VIDEO);
        testResult4.setCategory(Category.IMAGE);

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        
        // Apply category filter.
        model.setFilterEditor(new AbstractMatcherEditor<VisualSearchResult>() {
            Matcher<VisualSearchResult> matcher = new Matcher<VisualSearchResult>() {
                @Override
                public boolean matches(VisualSearchResult item) {
                    return item.getCategory() == Category.VIDEO;
                }
            };
            
            @Override
            public Matcher<VisualSearchResult> getMatcher() {
                return matcher;
            }
        });
        waitForUiThread();
        
        // Get filtered search results.
        List<VisualSearchResult> filteredList = model.getFilteredSearchResults();
        
        // Verify filtered list.
        int expectedSize = 1;
        int actualSize = filteredList.size();
        assertEquals("filtered list size", expectedSize, actualSize);

    }
    
    /** Tests method to retrieve sorted and filtered search results. */
    public void testGetSortedSearchResults() {

        final PropertiableHeadings propertiableHeadings = context.mock(PropertiableHeadings.class);

        context.checking(new Expectations(){
            {
                allowing(provider).get();
                will(returnValue(propertiableHeadings));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("xray"));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("zulu"));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("whiskey"));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("yankee"));
            }
        });
        
        // Create test search results.
        TestSearchResult testResult1 = new TestSearchResult("1", "xray");
        TestSearchResult testResult2 = new TestSearchResult("2", "zulu");
        TestSearchResult testResult3 = new TestSearchResult("3", "whiskey");
        TestSearchResult testResult4 = new TestSearchResult("4", "yankee");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        
        // Get sorted search results.
        model.setSelectedCategory(SearchCategory.ALL);
        waitForUiThread();
        List<VisualSearchResult> sortedList = model.getSortedSearchResults();

        // Verify unsorted order.
        String expectedReturn = "xray";
        String actualReturn = sortedList.get(0).getHeading();
        assertEquals("unsorted list", expectedReturn, actualReturn);
        
        // Apply sort option.
        model.setSortOption(SortOption.NAME);
        waitForUiThread();
        
        // Verify sorted order.
        expectedReturn = "whiskey";
        actualReturn = sortedList.get(0).getHeading();
        assertEquals("sorted list", expectedReturn, actualReturn);

    }

    /** Tests method to set filter editor with filter text. */
    public void testSetFilterEditor() {

        // Create test search results.
        TestSearchResult testResult1 = new TestSearchResult("1", "xray");
        TestSearchResult testResult2 = new TestSearchResult("2", "zulu");
        TestSearchResult testResult3 = new TestSearchResult("3", "whiskey");
        TestSearchResult testResult4 = new TestSearchResult("4", "yankee");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        
        // Get all search results.
        List<VisualSearchResult> filteredList = model.getFilteredSearchResults();
        
        // Verify unfiltered list.
        int expectedSize = 4;
        int actualSize = filteredList.size();
        assertEquals("unfiltered list size", expectedSize, actualSize);
        
        // Apply filter editor.
        TextMatcherEditor<VisualSearchResult> editor = new TextMatcherEditor<VisualSearchResult>(
                new VisualSearchResultTextFilterator());
        editor.setFilterText(new String[] {"z"});
        model.setFilterEditor(editor);
        waitForUiThread();
        
        // Verify filtered list.
        expectedSize = 1;
        actualSize = filteredList.size();
        assertEquals("filtered list size", expectedSize, actualSize);

    }
   
    /**
     * Test implementation of Search.
     */
    private static class TestSearch implements Search {

        @Override
        public void addSearchListener(SearchListener searchListener) {
        }

        @Override
        public void removeSearchListener(SearchListener searchListener) {
        }

        @Override
        public SearchCategory getCategory() {
            return null;
        }

        @Override
        public void repeat() {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public CopyOnWriteArrayList<SearchListener> getListenerList() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getQuery() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public GUID getQueryGuid() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    
    /**
     * Test implementation of SearchInfo.
     */
    private static class TestSearchInfo implements SearchInfo {

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public Map<FilePropertyKey, String> getAdvancedDetails() {
            return null;
        }

        @Override
        public SearchCategory getSearchCategory() {
            return null;
        }

        @Override
        public String getSearchQuery() {
            return null;
        }

        @Override
        public SearchType getSearchType() {
            return null;
        }
    }
}
