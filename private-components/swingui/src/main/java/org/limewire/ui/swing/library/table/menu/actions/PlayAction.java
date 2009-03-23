package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.Catalog;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Plays the given file in the limewire player, 
 * or launches it depending on if it is supported.
 */
public class PlayAction extends AbstractAction {
    
    private final LibraryNavigator libraryNavigator;
    private final Catalog catalog;
    private final LocalFileItem localFile;
    
    public PlayAction(LibraryNavigator libraryNavigator, Catalog catalog, 
            LocalFileItem localFile) {
        super(I18n.tr("Play"));
        this.libraryNavigator = libraryNavigator;
        this.catalog = catalog;
        this.localFile = localFile;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Set active catalog.
        libraryNavigator.setActiveCatalog(catalog);
        // Play file.
        PlayerUtils.playOrLaunch(localFile.getFile());
    }
}