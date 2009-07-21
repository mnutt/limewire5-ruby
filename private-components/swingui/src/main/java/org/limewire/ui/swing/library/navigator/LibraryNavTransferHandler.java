package org.limewire.ui.swing.library.navigator;

import java.awt.Point;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.warnings.LibraryWarningController;

import com.google.inject.Inject;

class LibraryNavTransferHandler extends LocalFileListTransferHandler {
    private LocalFileList localFileList = null;
    
    @Inject
    public LibraryNavTransferHandler(LibraryWarningController librarySupport) {
        super(librarySupport);
    }
    
    @Override
    public boolean canImport(TransferSupport info) {
        if (info.getComponent() instanceof LibraryNavigatorTable) {
            LibraryNavigatorTable libraryNavigatorTable = (LibraryNavigatorTable) info.getComponent();
            DropLocation dropLocation = info.getDropLocation();
            Point point = dropLocation.getDropPoint();
            
            int column = libraryNavigatorTable.columnAtPoint(point);
            int row = libraryNavigatorTable.rowAtPoint(point);    
            if(column < 0 || row < 0) {
                return false;
            }
            
            LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.getValueAt(row, column);
            if (libraryNavItem != null) {
                this.localFileList = libraryNavItem.getLocalFileList();
                return super.canImport(info);
            }
        }
        
        return false;
    }

    @Override
    public boolean importData(TransferSupport info) {
        if(info.getComponent() instanceof LibraryNavigatorTable) {
            LibraryNavigatorTable libraryNavigatorTable = (LibraryNavigatorTable) info.getComponent();    
            DropLocation dropLocation = info.getDropLocation();
            Point point = dropLocation.getDropPoint();
            int column = libraryNavigatorTable.columnAtPoint(point);
            int row = libraryNavigatorTable.rowAtPoint(point);
            
            if(column < 0 || row < 0) {
                return false;
            }
            
            LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.getValueAt(row, column);
            if (libraryNavItem != null) {
                this.localFileList = libraryNavItem.getLocalFileList();            
                return super.importData(info);
            }
        }
        
        return false;
    }

    @Override
    public LocalFileList getLocalFileList() {
        return localFileList;
    }
    
    @Override
    protected List<File> getSelectedFiles() {
        return Collections.emptyList();
    }
}
