package org.limewire.core.impl.bittorrent;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;

import com.google.inject.AbstractModule;


public class MockBittorrentModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(TorrentManager.class).to(MockTorrentManager.class);
        bind(TorrentSettings.class).annotatedWith(TorrentSettingsAnnotation.class).to(MockTorrentSettings.class);
    }

}
