package com.limegroup.bittorrent;

import java.io.File;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.libtorrent.LazyTorrentManager;
import org.limewire.libtorrent.TorrentImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.limegroup.bittorrent.metadata.TorrentMetaReader;

public class LimeWireBittorrentModule extends AbstractModule {

    @Override
    protected void configure() {
        // bound eagerly so it registers itself with MetaDataFactory
        bind(TorrentMetaReader.class).asEagerSingleton();
        bind(Torrent.class).to(TorrentImpl.class);
        bind(TorrentSettings.class).annotatedWith(TorrentSettingsAnnotation.class).toProvider(new Provider<TorrentSettings>() {
           @Override
            public TorrentSettings get() {
               return new TorrentSettings() {

                @Override
                public int getMaxDownloadBandwidth() {
                    int download_speed = BittorrentSettings.LIBTORRENT_DOWNLOAD_SPEED.getValue();
                    if (download_speed >= 100) {
                        return 0;
                    }
                    int limit = (ConnectionSettings.CONNECTION_SPEED.getValue() / 8) * 1024 * download_speed / 100;
                    return limit;
                }

                @Override
                public int getMaxUploadBandwidth() {
                    int upload_speed = BittorrentSettings.LIBTORRENT_UPLOAD_SPEED.getValue();
                    if (upload_speed >= 100) {
                        return 0;
                    }
                    int limit = (ConnectionSettings.CONNECTION_SPEED.getValue() / 8) * 1024 * upload_speed / 100;
                    return limit;
                }

                @Override
                public File getTorrentDownloadFolder() {
                    return SharingSettings.INCOMPLETE_DIRECTORY.get();
                }

                @Override
                public boolean isTorrentsEnabled() {
                    return BittorrentSettings.LIBTORRENT_ENABLED.getValue();
                }
                
                @Override
                public boolean isReportingLibraryLoadFailture() {
                    return BittorrentSettings.LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE.getValue();
                }
                
                @Override
                public void setReportingLibraryLoadFailure(boolean reportingLibraryLoadFailure) {
                    BittorrentSettings.LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE.setValue(reportingLibraryLoadFailure);
                }
                
               };
           }
        });
        bind(TorrentManager.class).to(LazyTorrentManager.class);
    }
}
