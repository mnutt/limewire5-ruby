package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.util.Collection;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.MultiFileShareModel;
import org.limewire.ui.swing.util.I18n;

public class MultiFileShareWidget implements ShareWidget<LocalFileItem[]>{
    private LibrarySharePanel sharePanel;
    
    public MultiFileShareWidget(ShareListManager shareListManager, Collection<Friend> allFriends, ShapeDialog shapeDialog){
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog);
        sharePanel.setShareModel(new MultiFileShareModel(shareListManager));
    }
    
    @Override
    public void show(Component c) {
        sharePanel.show(null);
    }
    
    @Override
    public void setShareable(LocalFileItem[] files){
        ((MultiFileShareModel)sharePanel.getShareModel()).setFileItems(files);
        sharePanel.setTitleLabel(I18n.tr("Share {0} {1} files", files.length, files[0].getCategory().toString()));
        sharePanel.setTopLabel(I18n.tr("Shared {0} files with:", files.length));
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
