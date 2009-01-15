package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.io.File;

import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.FileShareModel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.impl.ThreadSafeList;

public class FileShareWidget implements ShareWidget<File> {

    private LibrarySharePanel sharePanel;
    private File file;
    private ShareListManager shareListManager;

    public FileShareWidget(ShareListManager shareListManager, ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog) {
        this.shareListManager = shareListManager;
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog);
        sharePanel.setTopLabel(I18n.tr("Currently sharing with"));
        sharePanel.setTitleLabel(I18n.tr("Share one file"));
    }

    public void show(Component c) {
        sharePanel.show(c, new FileShareModel(shareListManager, file));
    }

    public void setShareable(File file) {
        this.file = file;
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
