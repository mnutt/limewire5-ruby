package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.ui.swing.util.I18n;

public class DefaultSearchInfo implements SearchInfo {

    private final String query;
    private final SearchCategory searchCategory;
    private final SearchType searchType;
    
    public static DefaultSearchInfo createKeywordSearch(String query, SearchCategory searchCategory) {
        return new DefaultSearchInfo(query, searchCategory, SearchType.KEYWORD);
    }
    
    public static DefaultSearchInfo createWhatsNewSearch(SearchCategory searchCategory) {
        return new DefaultSearchInfo(I18n.tr("What's New"), searchCategory, SearchType.WHATS_NEW);
    }

    private DefaultSearchInfo(String query, SearchCategory searchCategory, SearchType searchType) {
        this.query = query;
        this.searchCategory = searchCategory;
        this.searchType = searchType;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public String getTitle() {
        return query;
    }
    
    @Override
    public SearchCategory getSearchCategory() {
        return searchCategory;
    }
    
    @Override
    public SearchType getSearchType() {
        return searchType;
    }

}
