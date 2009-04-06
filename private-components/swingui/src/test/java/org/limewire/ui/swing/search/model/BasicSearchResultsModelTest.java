package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.Assert;

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
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.matchers.TextMatcherEditor;

/**
 * Test case for BasicSearchResultsModel. 
 */
public class BasicSearchResultsModelTest extends BaseTestCase {
    /** Instance of class being tested. */
    private BasicSearchResultsModel model;

    /**
     * Constructs a test case for the specified method name.
     */
    public BasicSearchResultsModelTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Create test instance.
        model = new BasicSearchResultsModel(new TestSearchInfo(), 
                new TestSearch(), new TestPropertiableHeadings(), null, null);
    }
    
    @Override
    protected void tearDown() throws Exception {
        model = null;
        super.tearDown();
    }

    public void testGroupingByName2UrnsNameComesEarly() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("1", "other file");
        // other file for urn1 is coming in early
        TestSearchResult testResult3 = new TestSearchResult("2", "other file");
        TestSearchResult testResult4 = new TestSearchResult("1", "file name");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(3, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
    }

    public void testGroupingByName2UrnsNameComesLate() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("1", "file name");
        // other file for urn1 is coming in late
        TestSearchResult testResult4 = new TestSearchResult("1", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(3, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(1, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        Assert.assertNull(group1.getSimilarityParent());
        Assert.assertEquals(group1, group0.getSimilarityParent());
    }

    public void testGroupingByName2UrnsNameComesLateMultipleAdds() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("1", "file name");
        TestSearchResult testResult4 = new TestSearchResult("1", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(9, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(1, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(3, coreResults1.size());

        Assert.assertNull(group1.getSimilarityParent());
        Assert.assertEquals(group1, group0.getSimilarityParent());
    }

    public void testGroupByName4Urns() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "other file");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("3", "other file");
        TestSearchResult testResult4 = new TestSearchResult("4", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(4, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(3, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(1, coreResults2.size());

        VisualSearchResult group3 = results.get(3);
        List<VisualSearchResult> similarResults3 = group3.getSimilarResults();
        Assert.assertEquals(0, similarResults3.size());
        List<SearchResult> coreResults3 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults3.size());

        Assert.assertEquals(group0, group1.getSimilarityParent());
        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group2.getSimilarityParent());
        Assert.assertEquals(group0, group3.getSimilarityParent());

    }

    public void testGroupingByName3Urns() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "other file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "other file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);

        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(2, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(1, coreResults2.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
        Assert.assertEquals(group0, group2.getSimilarityParent());
    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        model.addSearchResult(testResult6);

        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(2, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(2, coreResults2.size());

        Assert.assertNull(group1.getSimilarityParent());
        Assert.assertEquals(group1, group0.getSimilarityParent());
        Assert.assertEquals(group1, group2.getSimilarityParent());
    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty3GroupHasMoreFiles() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");
        TestSearchResult testResult7 = new TestSearchResult("3", "blah3 file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        model.addSearchResult(testResult6);
        model.addSearchResult(testResult7);

        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(2, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(3, coreResults2.size());

        Assert.assertNull(group1.getSimilarityParent());
        Assert.assertEquals(group1, group2.getSimilarityParent());
        Assert.assertEquals(group1, group0.getSimilarityParent());
    }

    public void testVisibility() {
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        
        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah1 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");

        
        model.addSearchResult(testResult1);
        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(1, results.size());
        VisualSearchResult result0 = results.get(0);
        Assert.assertTrue(result0.isVisible());
        Assert.assertFalse(result0.isChildrenVisible());
        result0.setChildrenVisible(true);
        Assert.assertTrue(result0.isVisible());
        Assert.assertTrue(result0.isChildrenVisible());

        model.addSearchResult(testResult2);
        Assert.assertTrue(result0.isVisible());
        Assert.assertTrue(result0.isChildrenVisible());

        model.addSearchResult(testResult3);

        Assert.assertTrue(result0.isVisible());
        Assert.assertTrue(result0.isChildrenVisible());
        List<VisualSearchResult> children = result0.getSimilarResults();
        Assert.assertEquals(1, children.size());
        VisualSearchResult child = children.get(0);
        Assert.assertTrue(child.isVisible());

        result0.setChildrenVisible(false);
        Assert.assertTrue(result0.isVisible());
        Assert.assertFalse(result0.isChildrenVisible());
        Assert.assertFalse(child.isVisible());
        result0.setChildrenVisible(true);

        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);

        children = result0.getSimilarResults();
        Assert.assertEquals(2, children.size());
        VisualSearchResult child0 = children.get(0);
        VisualSearchResult child1 = children.get(1);
        Assert.assertTrue(child0.isVisible());
        Assert.assertTrue(child1.isVisible());
        result0.setChildrenVisible(false);

        model.addSearchResult(testResult6);

        children = result0.getSimilarResults();
        Assert.assertEquals(2, children.size());
        child0 = children.get(0);
        child1 = children.get(1);
        Assert.assertFalse(child0.isVisible());
        Assert.assertFalse(child1.isVisible());
        result0.setChildrenVisible(true);
        Assert.assertTrue(child0.isVisible());
        Assert.assertTrue(child1.isVisible());

        results = model.getGroupedSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(2, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(2, coreResults2.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group2.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
    }
    
    public void testSameNameHyphenNameHyphenName() {
       
        
        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test-foo-bar");
        SearchResult searchResult1 = new TestSearchResult("1", "test-foo-bar.mp3", properties1); 
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "test-foo-bar");
        SearchResult searchResult2 = new TestSearchResult("2", "test-foo-bar.mp3", properties2);
        
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        model.addSearchResult(searchResult1);
        model.addSearchResult(searchResult2);
        
        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
    }
    
    public void testNotSameNameOrButSameTrackMetaData() {
        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test");
        SearchResult searchResult1 = new TestSearchResult("1", "test.mp3", properties1);
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "blah123");
        properties2.put(FilePropertyKey.TITLE, "test");
        SearchResult searchResult2 = new TestSearchResult("2", "blah123.mp3", properties2);
        
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        model.addSearchResult(searchResult1);
        model.addSearchResult(searchResult2);
        
        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        //should be no similar results
        Assert.assertNull(group1.getSimilarityParent());
        Assert.assertNull(group0.getSimilarityParent());

    
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
        
        
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        model.addSearchResult(searchResult1);
        model.addSearchResult(searchResult2);
        
        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
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
        
        model.getGroupedSearchResults()
                .addListEventListener(
                        new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        model.addSearchResult(searchResult1);
        model.addSearchResult(searchResult2);
        
        List<VisualSearchResult> results = model.getGroupedSearchResults();
        Assert.assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        Assert.assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
    }

    /** Tests method to retrieve filtered search results by category. */
    public void testGetCategorySearchResults() {
        // Create test search results.
        TestSearchResult testResult1 = new TestSearchResult("1", "xray");
        TestSearchResult testResult2 = new TestSearchResult("2", "zulu");
        TestSearchResult testResult3 = new TestSearchResult("3", "whiskey");
        TestSearchResult testResult4 = new TestSearchResult("4", "yankee");
        testResult3.setCategory(Category.VIDEO);
        testResult4.setCategory(Category.IMAGE);

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        // Get category search results.
        List<VisualSearchResult> categoryList = model.getCategorySearchResults(SearchCategory.VIDEO);
        
        // Verify category list.
        int expectedSize = 1;
        int actualSize = categoryList.size();
        assertEquals("category list size", expectedSize, actualSize);
    }
    
    /** Tests method to retrieve sorted and filtered search results. */
    public void testGetSortedSearchResults() {
        // Create test search results.
        TestSearchResult testResult1 = new TestSearchResult("1", "xray");
        TestSearchResult testResult2 = new TestSearchResult("2", "zulu");
        TestSearchResult testResult3 = new TestSearchResult("3", "whiskey");
        TestSearchResult testResult4 = new TestSearchResult("4", "yankee");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        
        // Get sorted search results.
        model.setSelectedCategory(SearchCategory.ALL);
        List<VisualSearchResult> sortedList = model.getSortedSearchResults();
        
        // Verify unsorted order.
        String expectedReturn = "xray";
        String actualReturn = sortedList.get(0).getHeading();
        assertEquals("unsorted list", expectedReturn, actualReturn);
        
        // Apply sort option.
        model.setSortOption(SortOption.NAME);
        
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

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        
        // Get all search results.
        List<VisualSearchResult> filteredList = model.getCategorySearchResults(SearchCategory.ALL);
        
        // Verify unfiltered list.
        int expectedSize = 4;
        int actualSize = filteredList.size();
        assertEquals("unfiltered list size", expectedSize, actualSize);
        
        // Apply filter editor.
        TextMatcherEditor<VisualSearchResult> editor = new TextMatcherEditor<VisualSearchResult>(
                new VisualSearchResultTextFilterator());
        editor.setFilterText(new String[] {"z"});
        model.setFilterEditor(editor);
        
        // Verify filtered list.
        expectedSize = 1;
        actualSize = filteredList.size();
        assertEquals("filtered list size", expectedSize, actualSize);
    }

    /**
     * Test implementation of PropertiableHeadings.
     */
    private static class TestPropertiableHeadings implements PropertiableHeadings {

        @Override
        public String getHeading(PropertiableFile propertiable) {
            Object name = propertiable.getProperty(FilePropertyKey.NAME);
            return (name == null) ? "" : name.toString();
        }

        @Override
        public String getSubHeading(PropertiableFile propertiable) {
            Object name = propertiable.getProperty(FilePropertyKey.AUTHOR);
            return (name == null) ? "" : name.toString();
        }
        
        @Override
        public String getFileSize(PropertiableFile propertiable) {
            return "0";
        }

        @Override
        public String getLength(PropertiableFile propertiable) {
            return "0";
        }

        @Override
        public String getQualityScore(PropertiableFile propertiableFile) {
            return "0";
        }
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
