package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Tries to safely launch the given file.
 * If it is an allowed file type it will be launched, 
 * otherwise explorer will be opened to the files location
 */
class LaunchFileAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final Provider<LibraryNavigatorPanel> libraryNavProvider;
    private final Provider<PlayerMediator> playerProvider;
    
    @Inject
    public LaunchFileAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems,
        Provider<LibraryNavigatorPanel> libraryNavProvider,
        Provider<PlayerMediator> playerProvider) {
        super(I18n.tr("Play/Open"));

        this.selectedLocalFileItems = selectedLocalFileItems;
        this.libraryNavProvider = libraryNavProvider;
        this.playerProvider = playerProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        if (localFileItems.size() > 0) {
            // Get first selected item.
            LocalFileItem fileItem = localFileItems.get(0);
            
            if (SwingUiSettings.PLAYER_ENABLED.getValue() && PlayerUtils.isPlayableFile(fileItem.getFile())) {
                // Set active playlist and play file item.
                playerProvider.get().setActivePlaylist(libraryNavProvider.get().getSelectedNavItem());
                playerProvider.get().play(fileItem);
            } else {    
                NativeLaunchUtils.safeLaunchFile(fileItem.getFile());
            }
        }
    }
}
