/**
 * 
 */
package org.limewire.ui.swing.search;

import java.util.HashMap;
import java.util.Map;

public enum SearchViewType {
    LIST(0), TABLE(1);

    private static final Map<Integer, SearchViewType> modeById = new HashMap<Integer, SearchViewType>();

    static {
        modeById.put(LIST.getId(), LIST);
        modeById.put(TABLE.getId(), TABLE);
    }

    private final int id;

    SearchViewType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Returns the search view type for the given search view id. If non match,
     * LIST type is returned by default. This is used to save values in the
     * SearchSettings.SEARCH_VIEW_TYPE_ID
     */
    public static SearchViewType forId(int id) {
        SearchViewType searchViewType = modeById.get(id);
        if (searchViewType == null) {
            searchViewType = LIST;
        }
        return searchViewType;
    }
}