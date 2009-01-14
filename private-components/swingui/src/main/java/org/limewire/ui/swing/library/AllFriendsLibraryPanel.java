package org.limewire.ui.swing.library;

import java.awt.Color;

import javax.swing.JComponent;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

public class AllFriendsLibraryPanel extends AbstractFriendLibraryPanel {

    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride")
    private Color selectionPanelBackgroundOverride = null;    

    
    @Inject
    public AllFriendsLibraryPanel(
                    RemoteLibraryManager remoteLibraryManager,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    ButtonDecorator buttonDecorator,
                    ShareListManager shareListManager,
                    GhostDragGlassPane ghostPane,
                    LibraryNavigator libraryNavigator) {
        
        super(null, null, remoteLibraryManager.getAllFriendsFileList().getSwingModel(), 
                categoryIconManager, tableFactory, downloadListManager,
                libraryManager, headerBarFactory, ghostPane, libraryNavigator);
        
        GuiUtils.assignResources(this);
        
        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }
        
        createMyCategories(remoteLibraryManager.getAllFriendsFileList().getSwingModel());
        selectFirstVisible();
        getHeaderPanel().setText(I18n.tr("Download from all friends"));
    }
    
    protected JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> filtered) {
        addFriendInfoBar(category, filtered);
        return super.createMyCategoryAction(category, filtered);
    }
}