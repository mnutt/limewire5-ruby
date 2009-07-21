package org.limewire.ui.swing.downloads.table;

import java.io.File;
import java.util.List;

import org.limewire.ui.swing.library.table.AddToListMenu;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class DownloadAddToListMenu extends AddToListMenu { 
    
    @Inject
    public DownloadAddToListMenu(@FinishedDownloadSelected Provider<List<File>> selectedFiles) {
        super(selectedFiles);
    }
}