package org.limewire.ui.swing.downloads.table;

import java.io.File;
import java.util.List;

import org.limewire.ui.swing.library.table.ShowInListMenu;

import com.google.inject.Inject;
import com.google.inject.Provider;


class DownloadShowInListMenu extends ShowInListMenu { 
    
    @Inject
    public DownloadShowInListMenu(@FinishedDownloadSelected Provider<List<File>> selectedFiles) {
        super(selectedFiles);
    }
}