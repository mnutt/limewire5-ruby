package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

/**
 * Removes given list of files from the library then trys to move them to the
 * trash or delete them.
 */
public class DeleteAction extends AbstractAction {

    private final LocalFileItem[] fileItemArray;

    private final LibraryManager libraryManager;

    public DeleteAction(final LocalFileItem[] fileItemArray, LibraryManager libraryManager) {
        super();
        String deleteName = I18n.tr("Delete Files");
        if(OSUtils.isAnyMac()) {
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
        BackgroundExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                for (LocalFileItem fileItem : fileItemArray) {
                    if (!fileItem.isIncomplete()) {
                        FileUtils.unlockFile(fileItem.getFile());
                        libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
                        FileUtils.delete(fileItem.getFile(), OSUtils.supportsTrash());
                    }
                }
            }
        });
    }
}