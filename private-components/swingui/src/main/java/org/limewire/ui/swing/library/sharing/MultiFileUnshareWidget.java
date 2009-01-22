package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.MultiFileUnshareModel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.impl.ThreadSafeList;

public class MultiFileUnshareWidget implements ShareWidget<LocalFileItem[]>{
    private LibrarySharePanel unsharePanel;
    private LocalFileItem[] files;
    private ShareListManager shareListManager;
    
    public MultiFileUnshareWidget(ShareListManager shareListManager, ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog){
        unsharePanel = new LibrarySharePanel(allFriends, shapeDialog);
        this.shareListManager = shareListManager;
        unsharePanel.setComboBoxVisible(false);
        unsharePanel.addShareListener(new ShareListener() {
            @Override
            public void sharingChanged(FriendShareEvent event) {
                if (unsharePanel.getShareCount() == 0) {
                    unsharePanel.setTitleLabel(I18n.tr("All {0} files were unshared", files.length));
                }
            }
        });

        shapeDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (unsharePanel.isShowing() && unsharePanel.getShareCount() == 0) {
                    unsharePanel.setTitleLabel(I18n.tr("These {0} files are not shared", files.length));
                }
            }
        });
    }
    
    public void show(Component c) {
        unsharePanel.show(null, new MultiFileUnshareModel(shareListManager, files));
    }
    

    @Override
    public void dispose() {
        unsharePanel.dispose();
    }

    @Override
    public void setShareable(LocalFileItem[] files) {
        this.files = files;
        unsharePanel.setTitleLabel(I18n.tr("Unshare {0} files", files.length));    
        unsharePanel.setTopLabel("with the following people");    
    }
}
