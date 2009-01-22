package org.limewire.ui.swing.downloads;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadMediator {

	/**
	 * unfiltered - common to all tables
	 */
	private final EventList<DownloadItem> commonBaseList;
	private DownloadListManager downloadListManager;
	
	@Inject
	public DownloadMediator(DownloadListManager downloadManager) {
	    this.downloadListManager = downloadManager;
		commonBaseList = GlazedListsFactory.filterList(downloadManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));
	}

	public void pauseAll() {
	    for (DownloadItem item : commonBaseList) {
            if (item.getState().isPausable()) {
                item.pause();
            }
        }
    }

	public void resumeAll() {
        for (DownloadItem item : commonBaseList) {
            if (item.getState().isResumable()) {
                item.resume();
            }
        }
    }
	
	public EventList<DownloadItem> getDownloadList() {
	    return this.commonBaseList;
	}
	
	public void clearFinished() {
	    downloadListManager.clearFinished();
	}
}
