package org.limewire.core.impl.upload;

import org.limewire.core.api.upload.UploadListManager;

import com.google.inject.AbstractModule;

public class MockUploadModule extends AbstractModule {
    
    @Override
    protected void configure() {
    	bind(UploadListManager.class).to(MockUploadListManager.class);
    }

}
