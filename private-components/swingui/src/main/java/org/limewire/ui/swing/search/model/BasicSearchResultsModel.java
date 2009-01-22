package org.limewire.ui.swing.search.model;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.PropertiableHeadings;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;

public class BasicSearchResultsModel implements SearchResultsModel {
    private final Log LOG = LogFactory.getLog(getClass());

    private final EventList<SearchResult> allSearchResults;

    private final AtomicInteger resultCount = new AtomicInteger();

    private ObservableElementList<VisualSearchResult> observableList;

    private FunctionList<List<SearchResult>, VisualSearchResult> groupedUrnResults;

    public BasicSearchResultsModel(PropertiableHeadings propertiableHeadings) {
        allSearchResults = new BasicEventList<SearchResult>();
        GroupingList<SearchResult> groupingListUrns = GlazedListsFactory.groupingList(
                allSearchResults, new UrnComparator());
        groupedUrnResults = GlazedListsFactory.functionList(
                groupingListUrns, new SearchResultGrouper(resultCount, propertiableHeadings));
        observableList = GlazedListsFactory.observableElementList(groupedUrnResults,
                GlazedLists.beanConnector(VisualSearchResult.class));
    }

    @Override
    public int getResultCount() {
        return resultCount.get();
    }

    @Override
    public EventList<VisualSearchResult> getObservableSearchResults() {
        return observableList;
    }

    @Override
    public EventList<VisualSearchResult> getGroupedSearchResults() {
        return groupedUrnResults;
    }

    @Override
    public void addSearchResult(SearchResult result) {
        if(result.getUrn() == null) {
            // Some results can be missing a URN, specifically
            // secure results.  For now, we drop these.
            // We should figure out a way to show them later on.
            return;
        }
        
        LOG.debugf("Adding result urn: {0} EDT: {1}", result.getUrn(), SwingUtilities
                .isEventDispatchThread());
        allSearchResults.add(result);
    }

    @Override
    public void removeSearchResult(SearchResult result) {
        allSearchResults.remove(result);
    }

    private static class UrnComparator implements Comparator<SearchResult> {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
            return o1.getUrn().compareTo(o2.getUrn());
        }
    }

    private static class SearchResultGrouper implements
            AdvancedFunction<List<SearchResult>, VisualSearchResult> {
        private final AtomicInteger resultCount;
        private final PropertiableHeadings propertiableHeadings;

        public SearchResultGrouper(AtomicInteger resultCount, PropertiableHeadings propertiableHeadings) {
            this.resultCount = resultCount;
            this.propertiableHeadings = propertiableHeadings;
        }

        @Override
        public void dispose(List<SearchResult> sourceValue, VisualSearchResult transformedValue) {
            resultCount.addAndGet(-transformedValue.getSources().size());
        }

        @Override
        public VisualSearchResult evaluate(List<SearchResult> sourceValue) {
            VisualSearchResult adapter = new SearchResultAdapter(sourceValue, propertiableHeadings);

            resultCount.addAndGet(adapter.getSources().size());
            return adapter;
        }

        @Override
        public VisualSearchResult reevaluate(List<SearchResult> sourceValue,
                VisualSearchResult transformedValue) {
            resultCount.addAndGet(-transformedValue.getSources().size());
            ((SearchResultAdapter) transformedValue).update();
            resultCount.addAndGet(transformedValue.getSources().size());
            return transformedValue;
        }
    }
}