package org.limewire.ui.swing.library.table.menu.actions;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SelectAllable;

public interface SharingActionFactory {

    public AbstractAction createShareGnutellaAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable);
    
    public AbstractAction createUnshareGnutellaAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable);
    
    public AbstractAction createShareFriendAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable);
    
    public AbstractAction createUnshareFriendAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable);
    
    public AbstractAction createDisabledFriendAction(String text);
}
