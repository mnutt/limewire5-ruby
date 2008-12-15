package com.limegroup.gnutella.gui.download;

import java.io.File;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.SharingSettings;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GuiCoreMediator;

/**
 *  Factory for downloading songs from the LimeWire Store (LWS)
 */
public class StoreDownloaderFactory implements GuiDownloaderFactory {

    /**
     * Location/hash of the file to download
     */
    private final RemoteFileDesc rfd;
    
    /**
     * Directory to save the download to
     */
    private File saveDir;
    
    /**
     * Name to save the file as
     */
    private String fileName;    

    public StoreDownloaderFactory(RemoteFileDesc rfd, String fileName) {        
        this.rfd = rfd;
        this.fileName = fileName != null ? fileName : rfd.getFileName();
        this.saveDir = SharingSettings.getSaveLWSDirectory();
    }
    

    
    /**
     * @return the file name/location to save the download
     */
    public File getSaveFile() {
        return new File(saveDir, fileName);
    }

    /**
     * Set the location and file name of the store download
     */
    public void setSaveFile(File saveFile) {
        if( saveFile != null && !saveFile.isDirectory()){
            this.fileName = saveFile.getName();
            this.saveDir = saveFile.getParentFile();
        } 
    }
    
    /**
     * @return the hash of the file
     */
    public URN getURN() {
        return rfd.getSHA1Urn();
    }
    
    /**
     * @return the file size
     */
    public long getFileSize() {
        return rfd.getSize();
    }
    
    /**
     * Performs a download and returns the id to query for the status of this download.
     * @return
     */
    public String download() {
        Downloader d = null;
        try {
            d = createDownloader(true);
        } catch (SaveLocationException e) {
            // We don't want an error message here, just let it go
        }      
        //
        // This used to be the id of the factory, but we
        // don't see the factory again, and only see the actual
        // downloader
        //
        return String.valueOf(System.identityHashCode(d));
    }         
    
    /**
     * Create a download that can be passed to the manager
     */
    public Downloader createDownloader(boolean overwrite)
    throws SaveLocationException {
        return GuiCoreMediator.getDownloadServices().downloadFromStore(rfd, overwrite, 
                  saveDir, fileName);
    }
}
