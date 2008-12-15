package org.limewire.ui.swing.upload;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.upload.table.UploadItemPropertiesFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class LimeWireUiUploadModule extends AbstractModule {
    
    @Override
    protected void configure() { 
        bind(new TypeLiteral<PropertiesFactory<UploadItem>>(){}).to(UploadItemPropertiesFactory.class);
    }
}
