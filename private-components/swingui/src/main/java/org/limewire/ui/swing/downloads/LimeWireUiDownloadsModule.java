package org.limewire.ui.swing.downloads;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.FinishedDownloadSelected;
import org.limewire.ui.swing.downloads.table.LimeWireUiDownloadsTableModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class LimeWireUiDownloadsModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireUiDownloadsTableModule());
        
        bind(DownloadHeaderPanel.class);            
    }
    
    
    @Provides @FinishedDownloadSelected List<File> selectedFiles(MainDownloadPanel downloadPanel) {
        List<DownloadItem> items = downloadPanel.getSelectedDownloadItems();
        List<File> files = new ArrayList<File>();
        
        for(DownloadItem item : items){
            if(item.getState() == DownloadState.DONE){
                files.addAll(item.getCompleteFiles());
            }
        }
        
        return files;
    } 

}
