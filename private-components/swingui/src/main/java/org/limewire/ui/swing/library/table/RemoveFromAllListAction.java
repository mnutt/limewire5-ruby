package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class RemoveFromAllListAction extends AbstractAction {
    private final Provider<List<File>> selectedFiles;
    private final SharedFileListManager manager;
    
    @Inject
    public RemoveFromAllListAction(@LibrarySelected Provider<List<File>> selectedFiles,
            SharedFileListManager manager) {
        super("Remove from All Lists");
        
        this.selectedFiles = selectedFiles;
        this.manager = manager;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final List<File> files = new ArrayList<File>(selectedFiles.get());
        
        int confirmation = FocusJOptionPane.showConfirmDialog(null, getMessage(files.size()), I18n.tr("Remove from Lists"), JOptionPane.OK_CANCEL_OPTION); 
        if (confirmation == JOptionPane.OK_OPTION) {
            final List<SharedFileList> sharedFileLists = new ArrayList<SharedFileList>(manager.getModel());
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    for(File file : files) {
                        for(SharedFileList sharedFileList : sharedFileLists)
                            sharedFileList.removeFile(file);
                    }
                }
            });
            
        }
    }
    
    private String getMessage(int selectedCount) {
        if(selectedCount == 1) {
            return I18n.tr("Remove this file from All Lists?");
        } else {
            return I18n.tr("Remove {0} files from All Lists?", selectedCount);            
        }
    }
}
