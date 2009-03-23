package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Creates a menu action for unsharing files with a friend. This spawns a
 * sharing dialog for choosing multiple friends. A boolean can be set 
 * for unsharing all the files in the selectable table.
 */
public class UnshareFriendAction extends AbstractAction {
    @Resource
    private Icon friendIcon;
    
    private final ShareWidgetFactory shareWidgetFactory;
    private final SelectAllable<LocalFileItem> librarySelectable;
    private final boolean isShareAll;
    
    public UnshareFriendAction(ShareWidgetFactory shareWidgetFactory, SelectAllable<LocalFileItem> librarySelectable, boolean isShareAll) {
        GuiUtils.assignResources(this);
        
        if(isShareAll) {
            putValue(Action.NAME, I18n.tr("Unshare folder with Friend"));
        } else {
            putValue(Action.NAME, I18n.tr("Unshare with Friend"));
        }
        putValue(Action.SMALL_ICON, friendIcon);
        
        this.shareWidgetFactory = shareWidgetFactory;
        this.librarySelectable = librarySelectable;
        this.isShareAll = isShareAll;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(isShareAll) {
            librarySelectable.selectAll();
        }
        List<LocalFileItem> selectedItems = librarySelectable.getSelectedItems();
        
        if (selectedItems.size() > 0) {
            ShareWidget<LocalFileItem[]> shareWidget = shareWidgetFactory.createMultiFileUnshareWidget();
            shareWidget.setShareable(selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
            shareWidget.show(GuiUtils.getMainFrame());
        }
    }
}
