package org.limewire.ui.swing.library;

import java.awt.Color;

import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
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
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

public class AllFriendsLibraryPanel extends AbstractFriendLibraryPanel {

    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride")
    private Color selectionPanelBackgroundOverride = null;    

    /**
     * Is true if there's any category showing and any category has 
     * a selected state, false otherwise.
     */
    private boolean isSelected = false;
    
    private EventList<RemoteFileItem> eventList;
    private CategorySelector categorySelector;
    
    @Inject
    public AllFriendsLibraryPanel(
                    final RemoteLibraryManager remoteLibraryManager,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    ButtonDecorator buttonDecorator,
                    ShareListManager shareListManager,
                    GhostDragGlassPane ghostPane,
                    LibraryNavigator libraryNavigator) {
        
        super(null, null, categoryIconManager, tableFactory, downloadListManager,
                libraryManager, headerBarFactory, ghostPane, libraryNavigator);
        
        GuiUtils.assignResources(this);
        
        eventList = remoteLibraryManager.getAllFriendsFileList().getSwingModel();
        
        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }
        
        createMyCategories(remoteLibraryManager.getAllFriendsFileList().getSwingModel());
        selectFirstVisible();
        getHeaderPanel().setText(I18n.tr("Download from all friends"));

        categorySelector = new CategorySelector();
        eventList.addListEventListener(categorySelector);
    }

    private void setSelected(boolean value) {
        isSelected = value;
    }
    
    private boolean isSelected() {
        return isSelected;
    }
    
    @Override
    public void dispose() {
        eventList.removeListEventListener(categorySelector);
        super.dispose();
    }
    
    /**
     * Listens to changes in the all friends file list. This list is updated as friends
     * sign on and off. If the list is populated after containing no files, the first
     * category is selected in the list. 
     */
    private class CategorySelector implements ListEventListener<RemoteFileItem> {
        @Override
        public void listChanged(ListEvent<RemoteFileItem> listChanges) {
            if(eventList.size() > 0 && isSelected() == false) {
                setSelected(true);
                // put back on the EDT to let the first file be added to
                // the list. The select the first category.
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        selectFirstVisible();
                    }
                });
            }
            if(eventList.size() == 0 && isSelected() == true) {
                setSelected(false);
            }
        }
    }

}