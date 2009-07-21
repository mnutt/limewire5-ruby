package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Removes given list of files from the library then tries to move them to the
 * trash or delete them.
 */
class DeleteAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final LibraryManager libraryManager;

    @Inject
    public DeleteAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            LibraryManager libraryManager) {
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.libraryManager = libraryManager;
        
        String deleteName = I18n.tr("Delete Files");
        if(OSUtils.isMacOSX()) {
            deleteName = I18n.tr("Move to Trash");
        } else if(OSUtils.isWindows()) {
            deleteName = I18n.tr("Move to Recycle Bin");
        }
        putValue(Action.NAME, deleteName);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        
        int confirmation = FocusJOptionPane.showConfirmDialog(null, getMessage(selectedItems.size()), I18n.tr("Delete File", "Delete Files", selectedItems.size()), JOptionPane.OK_CANCEL_OPTION); 
        if (confirmation == JOptionPane.OK_OPTION) {
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {                  
                    File currentSong = PlayerUtils.getCurrentSongFile();
                    for(LocalFileItem item : selectedItems) {
                        if(item.getFile().equals(currentSong)){
                            stopAudio();
                        }
                        if(!item.isIncomplete()) {
                            FileUtils.unlockFile(item.getFile());
                            libraryManager.getLibraryManagedList().removeFile(item.getFile());
                            FileUtils.delete(item.getFile(), OSUtils.supportsTrash());
                        }
                    }                    
                }
            });
        }
    }
    
    private void stopAudio() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                PlayerUtils.stop();
            }
        });
    }
    
    private String getMessage(int listSize) {
        return I18n.tr("Delete this file from disk?", "Delete these files from disk?", listSize);
    }
  
}