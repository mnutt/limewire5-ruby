package org.limewire.ui.swing.search;

import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class AlreadyDownloadedListEventListener implements ListEventListener<VisualSearchResult> {
    private final LibraryManager libraryManager;

    private final DownloadListManager downloadListManager;

    public AlreadyDownloadedListEventListener(LibraryManager libraryManager,
            DownloadListManager downloadListManager) {
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        LibraryFileList libraryFileList = libraryManager.getLibraryManagedList();
        final EventList<VisualSearchResult> eventList = listChanges.getSourceList();
        while (listChanges.next()) {
            boolean addOrUpdate = listChanges.getType() == ListEvent.INSERT
                    || listChanges.getType() == ListEvent.UPDATE;
            if (addOrUpdate) {
                final VisualSearchResult visualSearchResult = eventList.get(listChanges
                        .getIndex());
                //TODO should probably check more than just URN, can check the file save path as well.
                URN urn = visualSearchResult.getUrn();
                if (libraryFileList.contains(urn)) {
                    //first checking library for file
                    visualSearchResult.setDownloadState(BasicDownloadState.LIBRARY);
                } else {
                    //next checking download list
                    DownloadItem downloadItem = downloadListManager.getDownloadItem(urn);
                    if(downloadItem != null) {
                        downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(visualSearchResult));
                        visualSearchResult.setPreExistingDownload(true);
                        visualSearchResult.setDownloadState(BasicDownloadState.DOWNLOADING);
                    }
                }
            }
        }
    }
}
