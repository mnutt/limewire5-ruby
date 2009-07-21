package com.limegroup.mozilla;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.download.DownloadException;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

/**
 * Downloader listening to events from the Mozilla download process.
 */
public class MozillaDownloaderImpl extends AbstractCoreDownloader implements
        EventListener<DownloadStateEvent> {

    private final MozillaDownload download;

    private AtomicBoolean shouldBeRemoved = new AtomicBoolean(false);

    private final DownloadManager downloadManager;

    public MozillaDownloaderImpl(DownloadManager downloadManager, MozillaDownload download) {
        super(downloadManager);
        this.downloadManager = downloadManager;
        this.download = download;
        this.download.addListener(this);
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.MOZILLA;
    }

    @Override
    public GUID getQueryGUID() {
        return null;
    }

    @Override
    public void handleInactivity() {
        // nothing happens when we're inactive
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public boolean isQueuable() {
        return false;
    }

    @Override
    public void discardCorruptDownload(boolean delete) {
        finish();
    }

    @Override
    public long getAmountLost() {
        return 0;
    }

    @Override
    public int getAmountPending() {
        return (int) download.getAmountPending();
    }

    @Override
    public long getAmountRead() {
        return getAmountDownloaded();
    }

    private long getAmountDownloaded() {
        return download.getAmountDownloaded();
    }

    @Override
    public long getAmountVerified() {
        return getAmountDownloaded();
    }

    @Override
    public List<RemoteFileDesc> getRemoteFileDescs() {
        return Collections.emptyList();
    }

    @Override
    public int getBusyHostCount() {
        return 0;
    }

    @Override
    public int getChunkSize() {
        return 0;
    }

    @Override
    public long getContentLength() {
        return download.getContentLength();
    }

    @Override
    public String getCustomIconDescriptor() {
        return "";
    }

    @Override
    public File getDownloadFragment() {
        //not really used for this downloader
        return getIncompleteFile();
    }

    @Override
    public File getFile() {
        if(isCompleted()) {
            return getSaveFile();
        }
        return getIncompleteFile();
    }

    @Override
    public int getNumHosts() {
        return 1;
    }
    
    @Override
    public List<Address> getSourcesAsAddresses() {
        // TODO: Get the source from moz.
        return Collections.emptyList();
    }

    @Override
    public int getNumberOfAlternateLocations() {
        return 0;
    }

    @Override
    public int getNumberOfInvalidAlternateLocations() {
        return 0;
    }

    @Override
    public int getPossibleHostCount() {
        return 1;
    }

    @Override
    public int getQueuePosition() {
        return 1;
    }

    @Override
    public int getQueuedHostCount() {
        return 0;
    }

    @Override
    public int getRemainingStateTime() {
        return 0;
    }

    @Override
    public File getSaveFile() {
        String fileName = getIncompleteFile().getName();
        File saveFile = new File(SharingSettings.getSaveDirectory(fileName), fileName);
        return saveFile;
    }
    
    private File getIncompleteFile() {
        return download.getIncompleteFile();
    } 

    @Override
    public URN getSha1Urn() {
        return null;
    }

    @Override
    public DownloadState getState() {
        return download.getDownloadStatus();
    }

    @Override
    public int getTriedHostCount() {
        return 1;
    }

    @Override
    public String getVendor() {
        return "";
    }

    @Override
    public boolean isCompleted() {
        return download.isCompleted();
    }

    @Override
    public boolean isInactive() {
        return download.isInactive();
    }

    @Override
    public boolean isLaunchable() {
        return false;
    }

    @Override
    public boolean isPausable() {
        return !isPaused() && !isQueued();
    }

    @Override
    public boolean isPaused() {
        return download.isPaused();
    }

    @Override
    public boolean isRelocatable() {
        return false;
    }

    @Override
    public boolean isResumable() {
        return isPaused();
    }

    @Override
    public void pause() {
        if ((!isPaused() && !isInactive() && !isCompleted()) || isQueued()) {
            download.pauseDownload();
        }
    }

    private boolean isQueued() {
        return download.isQueued();
    }

    @Override
    public boolean resume() {
        if (isPaused() || isQueued()) {
            download.resumeDownload();
        }
        return true;
    }

    @Override
    public void stop() {
        finish();
        downloadManager.remove(this, true);
    }

    @Override
    protected File getDefaultSaveFile() {
        return null;
    }

    @Override
    public boolean conflicts(URN urn, long fileSize, File... files) {
        return false;
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        return false;
    }

    @Override
    public void finish() {
        download.cancelDownload();
        download.removeDownload();
        shouldBeRemoved.set(true);
    }

    @Override
    public void addListener(EventListener<DownloadStateEvent> listener) {
        this.download.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<DownloadStateEvent> listener) {
        return this.download.removeListener(listener);
    }

    @Override
    public void initialize() {
    }

    @Override
    public boolean shouldBeRemoved() {
        return shouldBeRemoved.get();
    }

    @Override
    public boolean shouldBeRestarted() {
        return false;
    }

    @Override
    public void startDownload() {
        // this is done automatically by mozilla.
    }

    @Override
    public float getAverageBandwidth() {
        return download.getAverageBandwidth();
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        return download.getMeasuredBandwidth();
    }

    @Override
    public void measureBandwidth() {
        download.measureBandwidth();
    }

    @Override
    protected DownloadMemento createMemento() {
        return null;
    }

    @Override
    public void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        // nothing we do not want to init from mementos
    }

    @Override
    public boolean isMementoSupported() {
        return false;
    }

    /**
     * Listens for events that tell use to remove item from download manager.
     */
    @Override
    public void handleEvent(DownloadStateEvent event) {
        DownloadState status = event.getType();
        if (status == DownloadState.COMPLETE || status == DownloadState.INVALID
                || status == DownloadState.ABORTED) {
            downloadManager.remove(this, false);
        }

        if (status == DownloadState.COMPLETE) {
            //move the finished file from the incomplete directory to the Save directory.
            File downloadedFile = getIncompleteFile();
            File savedFile = getSaveFile();
            FileUtils.forceDelete(savedFile);
            boolean success = FileUtils.forceRename(downloadedFile, savedFile);
           if(!success) {
               download.setDiskError();
           }
        }
    }

    @Override
    public void deleteIncompleteFiles() {
        FileUtils.delete(getIncompleteFile(), false);
    }
    
    @Override
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException {
        //overriding to track down cause of https://www.limewire.org/jira/browse/LWC-3697 remove when fixed
        super.setSaveFile(saveDirectory, fileName, overwrite);
    }
}
