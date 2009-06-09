package org.limewire.ui.swing.statusbar;

import javax.swing.BorderFactory;

import org.jdesktop.swingx.JXLabel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

class DownloadCountPanel extends JXLabel {
        
    @Inject
    DownloadCountPanel(DownloadListManager downloadListManager) {
        super("0");
        
        setName("DownloadCountPanel");
        setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        setIconTextGap(2);
        
        final EventList<DownloadItem> unfinishedDownloads = GlazedListsFactory.filterList(downloadListManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.DONE, DownloadState.ERROR));
        
        downloadListManager.getSwingThreadSafeDownloads().addListEventListener(new ListEventListener<DownloadItem>() {

            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                setText(Integer.toString(unfinishedDownloads.size()));
            }

        });
        
    }

}
