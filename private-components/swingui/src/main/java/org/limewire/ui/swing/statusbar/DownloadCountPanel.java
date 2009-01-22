package org.limewire.ui.swing.statusbar;

import javax.swing.BorderFactory;

import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class DownloadCountPanel extends JXLabel {
        
    @Inject
    DownloadCountPanel(DownloadListManager downloadListManager) {
        super("0");
        
        setName("DownloadCountPanel");
        setBorder(BorderFactory.createEmptyBorder(0,6,0,0));
        setIconTextGap(2);
        
        downloadListManager.getSwingThreadSafeDownloads().addListEventListener(new ListEventListener<DownloadItem>() {

            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                setText(Integer.toString(listChanges.getSourceList().size()));
            }

        });
        
    }

}
