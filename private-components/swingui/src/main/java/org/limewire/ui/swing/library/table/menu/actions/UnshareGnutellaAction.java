package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.sharing.model.MultiFileUnshareModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Creates a menu action for unsharing files with gnutella. This runs the
 * process in the background and does not spawn any other dialog. A 
 * boolean can be set for unsharing all the files in the selectable table.
 */
public class UnshareGnutellaAction extends AbstractAction {

    @Resource
    private Icon gnutellaIcon;
    
    private final ShareListManager shareListManager;
    private final SelectAllable<LocalFileItem> librarySelectable;
    private final boolean isShareAll;
    
    public UnshareGnutellaAction(ShareListManager shareListManager, SelectAllable<LocalFileItem> librarySelectable, boolean isShareAll) {
        GuiUtils.assignResources(this);
        
        if(isShareAll) {
            putValue(Action.NAME, I18n.tr("Unshare folder with the P2P Network"));
        } else {
            putValue(Action.NAME, I18n.tr("Unshare with the P2P Network"));
        }
        putValue(Action.SMALL_ICON, gnutellaIcon);
        
        this.shareListManager = shareListManager;
        this.librarySelectable = librarySelectable;
        this.isShareAll = isShareAll;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(isShareAll) {
            librarySelectable.selectAll();
        }
        List<LocalFileItem> selectedItems = librarySelectable.getSelectedItems();
        
        MultiFileUnshareModel model = new MultiFileUnshareModel(shareListManager, selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
        model.unshareFriend(SharingTarget.GNUTELLA_SHARE);
    }
}
