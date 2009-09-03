package com.limegroup.bittorrent;

import java.io.File;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UploadSettings;
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
        bind(TorrentManagerSettings.class).annotatedWith(TorrentSettingsAnnotation.class)
                .toProvider(new Provider<TorrentManagerSettings>() {
                    @Override
                    public TorrentManagerSettings get() {
                        return new TorrentManagerSettings() {

                            @Override
                            public int getMaxDownloadBandwidth() {
                                int download_speed = DownloadSettings.DOWNLOAD_SPEED.getValue();
                                if (download_speed >= 100) {
                                    return 0;
                                }
                                int limit = (ConnectionSettings.CONNECTION_SPEED.getValue() / 8)
                                        * 1024 * download_speed / 100;
                                return limit;
                            }

                            @Override
                            public int getMaxUploadBandwidth() {
                                int upload_speed = UploadSettings.UPLOAD_SPEED.getValue();
                                if (upload_speed >= 100) {
                                    return 0;
                                }
                                int limit = (ConnectionSettings.CONNECTION_SPEED.getValue() / 8)
                                        * 1024 * upload_speed / 100;
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
                                return BittorrentSettings.LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE
                                        .getValue();
                            }

                            @Override
                            public int getListenStartPort() {
                                return BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.getValue();
                            }

                            @Override
                            public int getListenEndPort() {
                                return BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.getValue();
                            }

                            @Override
                            public File getTorrentUploadsFolder() {
                                return BittorrentSettings.LIBTORRENT_UPLOADS_FOLDER.get();
                            }

                            @Override
                            public float getSeedRatioLimit() {

                                if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
                                    // fake unlimited value for using the
                                    return Float.MAX_VALUE;
                                }
                                return BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getValue();
                            }

                            @Override
                            public int getSeedTimeLimit() {
                                if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
                                    // fake unlimited value for using the
                                    return Integer.MAX_VALUE;
                                }
                                return BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getValue();
                            }

                            @Override
                            public float getSeedTimeRatioLimit() {
                                return BittorrentSettings.LIBTORRENT_SEED_TIME_RATIO_LIMIT
                                        .getValue();
                            }

                            @Override
                            public int getActiveDownloadsLimit() {
                                return BittorrentSettings.LIBTORRENT_ACTIVE_DOWNLOADS_LIMIT
                                        .getValue();
                            }

                            @Override
                            public int getActiveLimit() {
                                return BittorrentSettings.LIBTORRENT_ACTIVE_LIMIT.getValue();
                            }

                            @Override
                            public int getActiveSeedsLimit() {
                                return BittorrentSettings.LIBTORRENT_ACTIVE_SEEDS_LIMIT.getValue();
                            }
                        };
                    }
                });
        bind(TorrentManager.class).to(LazyTorrentManager.class);
    }
}
