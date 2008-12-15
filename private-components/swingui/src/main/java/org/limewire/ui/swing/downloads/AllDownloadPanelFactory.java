package org.limewire.ui.swing.downloads;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;

public interface AllDownloadPanelFactory {
    AllDownloadPanel create(EventList<DownloadItem> list);
}
