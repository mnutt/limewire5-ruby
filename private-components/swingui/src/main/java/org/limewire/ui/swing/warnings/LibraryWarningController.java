package org.limewire.ui.swing.warnings;

import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class acts as a go between for adding files from the ui to a fileList.
 * It has the correct logic for deciding whether or not a user needs to be
 * warned before a folder drop occurs.
 */
public class LibraryWarningController {
    private final Provider<LibraryWarningDialog> libraryWarningPanel;

    private final Provider<SharingWarningDialog> sharingWarningPanel;
    private final LibraryFileAdder libraryFileAdder;
    
    @Inject
    public LibraryWarningController(Provider<LibraryWarningDialog> libraryCategoryWarning,
            Provider<SharingWarningDialog> sharingWarningPanel, LibraryFileAdder libraryFileAdder) {
        this.libraryWarningPanel = libraryCategoryWarning;
        this.sharingWarningPanel = sharingWarningPanel;
        this.libraryFileAdder = libraryFileAdder;
    }

    public void addFiles(final LocalFileList fileList, final List<File> files) {

        int directoryCount = 0;
        for (File file : files) {
            if (file.isDirectory() && fileList.isFileAddable(file)) {
                directoryCount++;
                if (directoryCount > 1) {
                    // short circuit just need to know if there is more than 1
                    break;
                }
            }
        }

        SharedFileList sharedFileList = null;
        if (fileList instanceof SharedFileList) {
            sharedFileList = (SharedFileList) fileList;
        }

        if (directoryCount > 0 && LibrarySettings.ASK_ABOUT_FOLDER_DROP_CATEGORIES.getValue()) {
            LibraryWarningDialog panel = libraryWarningPanel.get();
            panel.initialize(fileList, files);
        } else if (directoryCount > 0 && SharingSettings.WARN_SHARING_FOLDER.getValue()
                && sharedFileList != null
                && (sharedFileList.isPublic() || sharedFileList.getFriendIds().size() > 0)) {
            SharingWarningDialog panel = sharingWarningPanel.get();
            panel.initialize(sharedFileList, files);
        } else {
            libraryFileAdder.addFilesInner(fileList, files);
        }
    }
}
