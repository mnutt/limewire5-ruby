package org.limewire.ui.swing.statusbar;

import javax.swing.BorderFactory;

import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class SharedFileCountPanel extends JXLabel {
        
    @Inject
    SharedFileCountPanel(ShareListManager shareListManager) {
        super(I18n.tr("Sharing {0} files", 0));
        
        this.setName("SharedFileCountPanel");
        
        this.setBorder(BorderFactory.createEmptyBorder(0,8,0,0));
        
        shareListManager.getCombinedShareList().getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>() {
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                setText(I18n.trn("Sharing {0} file", "Sharing {0} files", listChanges.getSourceList().size()));
            }
        });
        
    }

}
