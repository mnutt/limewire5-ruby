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
        String title;
        switch(searchCategory) {
        case AUDIO: title = I18n.tr("New audio"); break;
        case DOCUMENT: title = I18n.tr("New documents"); break;
        case IMAGE: title = I18n.tr("New images"); break;
        case PROGRAM: title = I18n.tr("New programs"); break;
        case VIDEO: title = I18n.tr("New videos"); break;            
        case OTHER:
        case ALL:
        default: title = I18n.tr("New files"); break;
        }
        return new DefaultSearchInfo(title, searchCategory, SearchType.WHATS_NEW);
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
