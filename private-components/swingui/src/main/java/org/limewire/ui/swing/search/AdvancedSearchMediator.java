package org.limewire.ui.swing.search;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.library.nav.NavMediator;
import org.limewire.ui.swing.search.advanced.AdvancedSearchPanel;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class AdvancedSearchMediator implements NavMediator<AdvancedSearchPanel> {

    public static final String NAME = "ADVANCED_SEARCH";
    
    private final Provider<AdvancedSearchPanel> advancedSearchPanel;
    private final SearchHandler searchHandler;
    private AdvancedSearchPanel advancedPanel;
    
    @Inject
    public AdvancedSearchMediator(Provider<AdvancedSearchPanel> advancedSearchPanel, SearchHandler searchHandler) {
        this.advancedSearchPanel = advancedSearchPanel;
        this.searchHandler = searchHandler;
    }
    
    @Override
    public AdvancedSearchPanel getComponent() {
        if(advancedPanel == null) {
            advancedPanel = advancedSearchPanel.get();
            advancedPanel.addSearchListener(new UiSearchListener() {
                @Override
                public void searchTriggered(SearchInfo searchInfo) {
                    searchHandler.doSearch(searchInfo);
                }
            });
        }
        return advancedPanel;
    }
}
