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

        putValue(Action.NAME, I18n.tr("Delete from Disk"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        
        String title = null;
        String message = null;
        String yesText = null;
        String noText = I18n.tr("Cancel");
        
        if (OSUtils.isWindows() && OSUtils.supportsTrash()) {
            title = I18n.trn("Move File to the Recycle Bin", "Move Files to the Recycle Bin", selectedItems.size());
            message = I18n.trn("Move this file to the Recycle Bin?", 
                    "Move this file to the Recycle Bin?", selectedItems.size());
            yesText = I18n.tr("Move to Recycle Bin");
        }
        else if (OSUtils.isMacOSX() && OSUtils.supportsTrash()) {
            title = I18n.trn("Move File to the Trash", "Move Files to the Trash", selectedItems.size());
            message = I18n.trn("Move this file to the Trash?", 
                    "Move this file to the Trash?", selectedItems.size());
            yesText = I18n.tr("Move to Trash");
        }
        else {
            title = I18n.trn("Delete File", "Delete Files", selectedItems.size());
            message = I18n.trn("Delete this file from disk?", "Delete these files from disk?", selectedItems.size());
            yesText = I18n.tr("Delete from Disk");
        }
        
        Object[] options = new Object[] {yesText, noText};
        
        int confirmation = FocusJOptionPane.showOptionDialog(null, 
                message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, noText);
        
        if (confirmation > -1 && options[confirmation] == yesText) {
            deleteSelectedItems(libraryManager, selectedItems);
        }
    }
    
    static void deleteSelectedItems(final LibraryManager libraryManager, final List<LocalFileItem> selectedItems) {
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
    
    private static void stopAudio() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                PlayerUtils.stop();
            }
        });
    }
  
}