package com.limegroup.gnutella.downloader;

import java.io.File;

import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;

public class DownloadSerialSettingsStub implements DownloadSerializeSettings {
    
    private final File backupFile;
    private final File saveFile;
    
    public DownloadSerialSettingsStub(File backup, File save) {
        this.backupFile = backup;
        this.saveFile = save;
    }

    
    @Override
    public File getBackupFile() {
        return backupFile;
    }
    
    @Override
    public File getSaveFile() {
        return saveFile;
    }
    
    
}
