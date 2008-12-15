package org.limewire.core.impl.download;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.download.SaveLocationManager;

import com.google.inject.AbstractModule;

public class MockDownloadModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SaveLocationManager.class).to(MockSaveLocationManager.class);
    	bind(DownloadListManager.class).to(MockDownloadListManager.class);
    	bind(ResultDownloader.class).to(MockDownloadListManager.class);
    }

}
