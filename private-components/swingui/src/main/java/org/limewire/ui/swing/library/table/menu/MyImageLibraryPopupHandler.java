package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.ui.swing.library.image.LibraryImageSubPanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactory;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.xmpp.api.client.XMPPService;

public class MyImageLibraryPopupHandler implements TablePopupHandler {
    private LibraryImageSubPanel imagePanel;

    private MyLibraryPopupMenu popupMenu;

    public MyImageLibraryPopupHandler(LibraryImageSubPanel imagePanel,
            SharingActionFactory sharingActionFactory, LibraryManager libraryManager, 
            PropertiesFactory<LocalFileItem> localFilePropFactory, XMPPService xmppService,
            LibraryNavigator libraryNavigator, PlaylistManager playlistManager) {
        this.imagePanel = imagePanel;
        this.popupMenu = new MyLibraryPopupMenu(Category.IMAGE,
                libraryManager, sharingActionFactory, localFilePropFactory, 
                xmppService, libraryNavigator, playlistManager);

    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {

        int popupRow = imagePanel.getImageList().locationToIndex(new Point(x, y));
        if(popupRow < 0)
            return;
        
        LocalFileItem selectedItem = (LocalFileItem) imagePanel.getImageList().getModel().getElementAt(popupRow);
        
        List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(imagePanel.getSelectedItems());

        if (selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
            selectedItems.clear();
            imagePanel.setSelectedIndex(popupRow);
        } 

        popupMenu.setSelectable(imagePanel);
        popupMenu.show(component, x, y);
    }
}
