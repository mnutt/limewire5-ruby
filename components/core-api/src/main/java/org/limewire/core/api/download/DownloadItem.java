package org.limewire.core.api.download;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.Address;

/**
 * A single download
 */
public interface DownloadItem extends PropertiableFile {	

	public static enum ErrorState {
	    NONE(I18nMarker.marktr("No problems.")),
	    DISK_PROBLEM(I18nMarker.marktr("There is a disk problem")),
	    CORRUPT_FILE(I18nMarker.marktr("The file is corrupted")),
	    FILE_NOT_SHARABLE(I18nMarker.marktr("This file is not shareable")),
	    UNABLE_TO_CONNECT(I18nMarker.marktr("Trouble connecting to people"));
	    
	    private final String message;

        private ErrorState(String message) {
            this.message = message;
        }
        
        /**
         * Returns error state message marked for translation.
         */
        public String getMessage() {
            return message;
        }
    };
    
    public final static long UNKNOWN_TIME = Long.MAX_VALUE;

	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	public DownloadState getState();

	public String getTitle();

	public int getPercentComplete();

	/**
	 * @return size in bytes
	 */
	public long getCurrentSize();
	
	/**
     * @return size in bytes
     */
	public long getTotalSize();

    /**
     * @return seconds remaining or <code>UNKNOWN</code> if unknown
     */
	public long getRemainingDownloadTime();
	
    /**
     * @return seconds remaining or <code>UNKNOWN</code> if unknown
     */
    public long getRemainingQueueTime();

	public void cancel();

	public void pause();

	public void resume();

	public int getDownloadSourceCount();

	public List<Address> getSources();
	
	public Category getCategory();

    /**
     * @return speed in kb/s or 0 if speed could not be measured
     */
	public float getDownloadSpeed();

	/**
     * Returns the position of the download on the uploader, relevant only if
     * the downloader is remote queued.
     */
    public int getQueuePosition();
  
    public ErrorState getErrorState();
    
    public int getLocalQueuePriority();
    
    /**
     * @return if this downloader can be launched or previewed.
     */
    public boolean isLaunchable();
    
    /**
     * Returns the File that is being downloaded to.
     * This call never blocks, but may return a file
     * that is locked and cannot be used by other programs.
     */
    File getDownloadingFile();
    
    /**
     * Returns a file suitable for launching.
     * This call may block while the file is created.
     */
    File getLaunchableFile();
}