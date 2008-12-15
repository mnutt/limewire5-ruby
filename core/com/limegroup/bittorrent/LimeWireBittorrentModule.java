package com.limegroup.bittorrent;

import com.google.inject.AbstractModule;
import com.limegroup.bittorrent.metadata.TorrentMetaReader;

public class LimeWireBittorrentModule extends AbstractModule {

    @Override
    protected void configure() {
        // bound eagerly so it registers itself with MetaDataFactory
        bind(TorrentMetaReader.class).asEagerSingleton();
    }

}
