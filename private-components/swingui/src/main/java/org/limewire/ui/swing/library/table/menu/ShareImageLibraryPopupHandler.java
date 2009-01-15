package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.image.LibraryImageSubPanel;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;

public class ShareImageLibraryPopupHandler implements TablePopupHandler {
    private LibraryImageSubPanel imagePanel;

    private ShareLibraryPopupMenu popupMenu;

    public ShareImageLibraryPopupHandler(LocalFileList friendFileList,
            LibraryImageSubPanel imagePanel, LibraryManager libraryManager,
            PropertiesFactory<LocalFileItem> localFilePropFactory) {
        this.imagePanel = imagePanel;
        this.popupMenu = new ShareLibraryPopupMenu(friendFileList, Category.IMAGE, libraryManager,
                localFilePropFactory);
    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {

        int popupRow = imagePanel.getImageList().locationToIndex(new Point(x, y));
        LocalFileItem selectedItem = (LocalFileItem) imagePanel.getImageList().getModel()
                .getElementAt(popupRow);

        List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(imagePanel
                .getSelectedItems());

        if (selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
            selectedItems.clear();
            imagePanel.setSelectedIndex(popupRow);
            selectedItems.add(selectedItem);
        }

        popupMenu.setFileItems(selectedItems);
        popupMenu.show(component, x, y);
    }
}
