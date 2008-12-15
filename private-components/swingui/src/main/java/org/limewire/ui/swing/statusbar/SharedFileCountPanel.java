package org.limewire.ui.swing.statusbar;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class SharedFileCountPanel extends JXLabel {
    
    // TODO: These resources should not be necessary, however?
    @Resource private Font font;
    @Resource private Color foreground;
    
    @Inject
    SharedFileCountPanel(ShareListManager shareListManager) {
        super(I18n.tr("Sharing {0} files", 0));
        
        GuiUtils.assignResources(this);
        
        this.setFont(this.font);
        this.setForeground(this.foreground);
        this.setBorder(BorderFactory.createEmptyBorder(0,8,0,0));
        
        shareListManager.getCombinedShareList().getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>() {
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                setText(I18n.tr("Sharing {0} files", listChanges.getSourceList().size()));
            }
        });
        
    }

}
