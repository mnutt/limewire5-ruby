package org.limewire.ui.swing.filter;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.filter.FilterableSource;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Test implementation of FilterableSource.
 */
public class MockFilterableSource implements FilterableSource<MockFilterableItem> {

    private final FilterDebugger<MockFilterableItem> filterDebugger;
    private final EventList<MockFilterableItem> unfilteredList;
    private final FilterList<MockFilterableItem> filteredList;
    private final SearchCategory filterCategory;
    
    public MockFilterableSource(SearchCategory searchCategory) {
        this.filterCategory = searchCategory;
        
        filterDebugger = new FilterDebugger<MockFilterableItem>();
        unfilteredList = new BasicEventList<MockFilterableItem>();
        filteredList = GlazedListsFactory.filterList(unfilteredList);
    }
    
    @Override
    public SearchCategory getFilterCategory() {
        return filterCategory;
    }
    
    @Override
    public FilterDebugger<MockFilterableItem> getFilterDebugger() {
        return filterDebugger;
    }

    @Override
    public EventList<MockFilterableItem> getFilteredList() {
        return filteredList;
    }

    @Override
    public EventList<MockFilterableItem> getUnfilteredList() {
        return unfilteredList;
    }

    @Override
    public void setFilterEditor(MatcherEditor<MockFilterableItem> editor) {
        filteredList.setMatcherEditor(editor);
    }
    
    /**
     * Adds the specified mock filterable item to the unfiltered list.
     */
    public void addItem(MockFilterableItem mockItem) {
        unfilteredList.add(mockItem);
    }
}
