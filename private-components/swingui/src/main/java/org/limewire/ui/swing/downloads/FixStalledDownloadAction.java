package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Attempts to fix all downloads that are in a stalled state.
 */
class FixStalledDownloadAction extends AbstractAction {
    private final Provider<DownloadMediator> downloadMediator;
    
    @Inject
    public FixStalledDownloadAction(Provider<DownloadMediator> downloadMediator) {
        super(I18n.tr("Fix Stalled"));
        
        this.downloadMediator = downloadMediator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        downloadMediator.get().fixStalled();
    } 
}