package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.model.SearchResultsModel;

public interface DownloadHandlerFactory {

    public DownloadHandler createDownloadHandler(SearchResultsModel searchResultsModel);
}
