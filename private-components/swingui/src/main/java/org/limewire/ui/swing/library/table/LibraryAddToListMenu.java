package org.limewire.ui.swing.library.table;

import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.LibrarySelected;

import com.google.inject.Inject;
import com.google.inject.Provider;


class LibraryAddToListMenu extends AddToListMenu { 
    
    @Inject
    public LibraryAddToListMenu(@LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            @LibrarySelected Provider<List<File>> selectedFiles) {
        super(selectedLocalFileList, selectedFiles);
    }
}