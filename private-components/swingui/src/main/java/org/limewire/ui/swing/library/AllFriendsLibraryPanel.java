package org.limewire.ui.swing.library;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class AllFriendsLibraryPanel extends AbstractFriendLibraryPanel {

    @Inject
    public AllFriendsLibraryPanel(
                    RemoteLibraryManager remoteLibraryManager,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    ButtonDecorator buttonDecorator) {
        super(remoteLibraryManager.getAllFriendsFileList().getSwingModel(), 
                categoryIconManager, tableFactory, downloadListManager,
                libraryManager, headerBarFactory);
        
        createMyCategories(remoteLibraryManager.getAllFriendsFileList().getSwingModel(), null);
        selectFirst();
        getHeaderPanel().setText(I18n.tr("Download from all friends"));
    }
}