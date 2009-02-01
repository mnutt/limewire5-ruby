package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.properties.PropertiesFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiDownloadsTableModule extends AbstractModule {

    @Override
    protected void configure() {
        
        bind(DownloadTableCellFactory.class).toProvider(
                FactoryProvider.newFactory(
                        DownloadTableCellFactory.class, DownloadTableCellImpl.class));
        
        bind(DownloadTableFactory.class).toProvider(
                FactoryProvider.newFactory(
                        DownloadTableFactory.class, DownloadTable.class));
        
        bind(DownloadActionHandler.class);
        
        bind(new TypeLiteral<PropertiesFactory<DownloadItem>>(){}).to(DownloadItemPropertiesFactory.class);
    }

}
