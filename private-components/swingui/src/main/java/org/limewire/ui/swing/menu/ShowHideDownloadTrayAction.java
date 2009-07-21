package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

class ShowHideDownloadTrayAction extends AbstractAction {
    private final String showDownloadTrayText = I18n.tr("Show Download Tray");
    private final String hideDownloadTrayText = I18n.tr("Hide Download Tray");

    @Inject
    public void register(DownloadMediator downloadMediator) {
        EventList<DownloadItem> downloadList = downloadMediator.getDownloadList();
        downloadList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                setEnabled(listChanges.getSourceList().size()  == 0);
                updateText();
            }
        });       
        setEnabled(downloadList.size() == 0);
        updateText();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY
                .setValue(!DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue());
        updateText();
    }

    private void updateText() {
        if(!isEnabled()) {
            this.putValue(Action.NAME, hideDownloadTrayText);
        } else if (DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue()) {
            this.putValue(Action.NAME, hideDownloadTrayText);
        } else {
            this.putValue(Action.NAME, showDownloadTrayText);
        }
    }
}