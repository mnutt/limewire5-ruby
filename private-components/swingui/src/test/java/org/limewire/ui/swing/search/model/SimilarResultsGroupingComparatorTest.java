package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.Collections;

import org.limewire.core.api.FilePropertyKey;

import junit.framework.TestCase;

public class SimilarResultsGroupingComparatorTest extends TestCase {
    private static final String NON_PARENT_1 = "nonParent1";
    private static final String NON_PARENT_2 = "nonParent2";
    private static final String SIMILAR1 = "similar1";
    private static final String SIMILAR2 = "similar2";
    private static final String PARENT = "parent";
    private ArrayList<VisualSearchResult> results;
    private MockVisualSearchResult parent;
    private VisualSearchResult simResult1;
    private VisualSearchResult simResult2;
    private MockVisualSearchResult nonParent1;
    private MockVisualSearchResult nonParent2;
    private SimilarResultsGroupingComparator comparator;

    @Override
    protected void setUp() {
        comparator = new SimilarResultsGroupingComparator() {
            @Override
            protected int doCompare(VisualSearchResult result1, VisualSearchResult result2) {
                return result1.getHeading().compareTo(result2.getHeading());
            }
        };

        results = new ArrayList<VisualSearchResult>();
        parent = new MockVisualSearchResult(PARENT);
        simResult1 = new MockVisualSearchResult(SIMILAR1, parent);
        simResult2 = new MockVisualSearchResult(SIMILAR2, parent);
        nonParent1 = new MockVisualSearchResult(NON_PARENT_1);
        nonParent2 = new MockVisualSearchResult(NON_PARENT_2);
    }

    public void testSorting1() {
        populate(nonParent2, simResult2, parent, nonParent1, simResult1);
        assertOrder(NON_PARENT_1, NON_PARENT_2,  PARENT, SIMILAR2, SIMILAR1);
    }

    public void testSorting2() {
        populate(nonParent1, simResult2, parent, simResult1, nonParent2);
        assertOrder(NON_PARENT_1, NON_PARENT_2, PARENT, SIMILAR2, SIMILAR1);
    }

    public void testSorting3() {
        populate(simResult2, simResult1, parent, nonParent1, nonParent2);
        assertOrder( NON_PARENT_1, NON_PARENT_2, PARENT, SIMILAR2, SIMILAR1);
    }

    public void testSorting4() {
        populate(simResult1, simResult2, nonParent1, parent, nonParent2);
        assertOrder(NON_PARENT_1, NON_PARENT_2, PARENT, SIMILAR1, SIMILAR2);
        
        results.clear();

        populate(parent, nonParent2, simResult2, nonParent1, simResult1);
        assertOrder(NON_PARENT_1, NON_PARENT_2, PARENT, SIMILAR2, SIMILAR1);
    }
    
    private void populate(VisualSearchResult... results) {
        for(VisualSearchResult result : results) {
            this.results.add(result);
        }
    }
    
    private void assertOrder(String... order) {
        Collections.sort(results, comparator);
        /*for(VisualSearchResult result : results) {
            System.out.print(result.getDescription() + ", ");
        }
        System.out.println("");*/
        
        int index = 0;
        for (String result : order) {
            assertEquals(result, results.get(index++).getProperty(FilePropertyKey.NAME));
        }
    }
}
