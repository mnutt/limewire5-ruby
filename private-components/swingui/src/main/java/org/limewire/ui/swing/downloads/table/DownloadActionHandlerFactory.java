package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;

public interface DownloadActionHandlerFactory {
    DownloadActionHandler create(EventList<DownloadItem> downloadItems);
}
