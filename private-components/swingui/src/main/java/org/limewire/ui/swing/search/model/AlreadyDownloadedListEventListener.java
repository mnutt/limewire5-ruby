package org.limewire.ui.swing.search.model;

import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * A listener to handle updates to the list of visual search results.  As each
 * VisualSearchResult is received, this listener sets its download state if 
 * the result is already in the library, or currently being downloaded.
 */
class AlreadyDownloadedListEventListener implements ListEventListener<VisualSearchResult> {

    private final LibraryManager libraryManager;

    private final DownloadListManager downloadListManager;

    /**
     * Constructs an AlreadyDownloadedListEventListener with the specified
     * library manager and download list manager.
     */
    public AlreadyDownloadedListEventListener(LibraryManager libraryManager,
            DownloadListManager downloadListManager) {
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
    }

    /**
     * Handles a listChanged event to update the state of each visual search
     * result associated with the event.
     */
    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        // Get list of library files, and list of search results. 
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
                    // Set download state when result is already in library.
                    visualSearchResult.setDownloadState(BasicDownloadState.LIBRARY);
                    
                } else {
                    // Set download state when result is being downloaded.
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
