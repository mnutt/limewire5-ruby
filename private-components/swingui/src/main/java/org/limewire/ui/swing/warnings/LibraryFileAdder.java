package org.limewire.ui.swing.warnings;

import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Clears any filters that may be on the library, then handles logic around
 * looping through a list of files and calling addFile or addFoler as necessary
 * on the provided LocalFileList.
 */
class LibraryFileAdder {

    private final Provider<LibraryMediator> libraryMediator;
    private final Provider<LibraryNavigatorTable> libraryNavigatorTable;

    @Inject
    public LibraryFileAdder(Provider<LibraryMediator> libraryMediator, Provider<LibraryNavigatorTable> libraryNavigatorTable) {
        this.libraryMediator = libraryMediator;
        this.libraryNavigatorTable = libraryNavigatorTable;
    }

    void addFilesInner(final LocalFileList fileList, final List<File> files) {
        
        //only clear the filters if the library has been initialized
        if(libraryMediator.get().isInitialized()) {
            //only clear the filters if we are adding files to the same list that is being shown
            LibraryNavItem libraryNavItem = libraryNavigatorTable.get().getSelectedItem();
            if(libraryNavItem != null && libraryNavItem.getLocalFileList() == fileList) {
                libraryMediator.get().clearFilters();
            }
        }
        for (File file : files) {
            if (fileList.isFileAddable(file)) {
                if (file.isDirectory()) {
                    fileList.addFolder(file);
                } else {
                    fileList.addFile(file);
                }
            }
        }
    }
}
