package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.MultiFileShareModel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.impl.ThreadSafeList;

public class MultiFileShareWidget implements ShareWidget<LocalFileItem[]>{
    private LibrarySharePanel sharePanel;
    private LocalFileItem[] files;
    private ShareListManager shareListManager;
    
    public MultiFileShareWidget(ShareListManager shareListManager, ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog){
        this.shareListManager = shareListManager;
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog);
    }
    
    @Override
    public void show(Component c) {
        sharePanel.show(null, new MultiFileShareModel(shareListManager, files));
    }
    
    @Override
    public void setShareable(LocalFileItem[] files){
        this.files = files;
        sharePanel.setTitleLabel(I18n.tr("Share {0} {1} files", files.length, files[0].getCategory().getSingularName()));
        sharePanel.setTopLabel(I18n.tr("Shared {0} files with:", files.length));
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
