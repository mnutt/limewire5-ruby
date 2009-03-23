package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Action;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

/**
 * Removes given list of files from the library then tries to move them to the
 * trash or delete them.
 */
public class DeleteAction extends AbstractAction {

    private final LocalFileItem[] fileItemArray;

    private final LibraryManager libraryManager;

    public DeleteAction(final LocalFileItem[] fileItemArray, LibraryManager libraryManager) {
        String deleteName = I18n.tr("Delete Files");
        if(OSUtils.isMacOSX()) {
            deleteName = I18n.tr("Move to Trash");
        } else if(OSUtils.isWindows()) {
            deleteName = I18n.tr("Move to Recycle Bin");
        }
        putValue(Action.NAME, deleteName);
        this.fileItemArray = fileItemArray;
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //PlayerUtils.getCurrentSongFile isn't threadsafe
        final File currentSong = PlayerUtils.getCurrentSongFile();
        BackgroundExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                for (LocalFileItem fileItem : fileItemArray) {
                    if(fileItem.getFile().equals(currentSong)){
                        stopAudio();
                    }
                    
                    if (!fileItem.isIncomplete()) {
                        FileUtils.unlockFile(fileItem.getFile());
                        libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
                        FileUtils.delete(fileItem.getFile(), OSUtils.supportsTrash());
                    }
                }
            }
        });
    }
    
    private void stopAudio() {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                PlayerUtils.stop();
            }
        });
    }
  
}