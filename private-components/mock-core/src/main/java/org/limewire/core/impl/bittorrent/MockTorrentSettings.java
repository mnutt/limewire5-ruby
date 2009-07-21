package org.limewire.core.impl.bittorrent;

import java.io.File;

import org.limewire.bittorrent.TorrentSettings;

public class MockTorrentSettings implements TorrentSettings {

    private boolean reportLibraryLoadFailure = false;
    
    @Override
    public int getMaxDownloadBandwidth() {
        return 0;
    }

    @Override
    public int getMaxUploadBandwidth() {
        return 0;
    }

    @Override
    public File getTorrentDownloadFolder() {
        return null;
    }

    @Override
    public boolean isTorrentsEnabled() {
        return true;
    }

    @Override
    public boolean isReportingLibraryLoadFailture() {
        return reportLibraryLoadFailure;
    }

    @Override
    public void setReportingLibraryLoadFailure(boolean reportingLibraryLoadFailure) {
        this.reportLibraryLoadFailure = reportingLibraryLoadFailure;
    }
}
