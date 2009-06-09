package org.limewire.ui.swing.downloads;

import org.limewire.ui.swing.downloads.table.LimeWireUiDownloadsTableModule;

import com.google.inject.AbstractModule;

public class LimeWireUiDownloadsModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireUiDownloadsTableModule());
        
        bind(DownloadHeaderPanel.class);            
    }

}
