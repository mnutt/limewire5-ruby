package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.io.File;
import java.util.Collection;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel.ShadowMode;
import org.limewire.ui.swing.library.sharing.model.FileShareModel;
import org.limewire.ui.swing.util.I18n;

public class FileShareWidget implements ShareWidget<File> {

   private LibrarySharePanel sharePanel;
    
    public FileShareWidget(ShareListManager shareListManager, Collection<Friend> allFriends, ShapeDialog shapeDialog){
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog, ShadowMode.SHADOW);
        sharePanel.setShareModel(new FileShareModel(shareListManager));
        sharePanel.setTopLabel(I18n.tr("Currently sharing with"));
        sharePanel.setTitleLabel(I18n.tr("Share one file"));
    }
    
    public void show(Component c) {
        sharePanel.show(c);
    }
    
    public void setShareable(File file){
        ((FileShareModel)sharePanel.getShareModel()).setFile(file);
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
