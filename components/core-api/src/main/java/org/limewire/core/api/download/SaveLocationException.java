package org.limewire.core.api.download;

import java.io.File;
import java.io.IOException;


/**
 * IOException which can be thrown from when setting
 * the save location on a downloader.
 */
public class SaveLocationException extends IOException {

    public static enum LocationCode {
        /** Attempt to change save location that violates security rules, such as attempting directory traversal .*/
        SECURITY_VIOLATION,
        /** Attempt to change save location too late to save file in new place */
        FILE_ALREADY_SAVED,
        /** Attempt to change save location to a directory where files cannot be created */
        DIRECTORY_NOT_WRITEABLE,
        /** Attempt to change save location to a non-existant directory */
        DIRECTORY_DOES_NOT_EXIST,
        /** Attempt to change save location to a File that already exists */
        FILE_ALREADY_EXISTS,
        /** Attempt to change save location to a file which is already reserved by another download */
        FILE_IS_ALREADY_DOWNLOADED_TO,
        /** Attempt to change save location to a pre-existing file that isn't a regular file (such as a directory or device file) */
        FILE_NOT_REGULAR,
        /** Attempt to change save directory to a "directory" that exists, but is not a directory */
        NOT_A_DIRECTORY,
        /** IOException or other filesystem error while setting save location. */
        FILESYSTEM_ERROR,
        /** 
         * Attempt to download the exact same file (urn, filename, size) while it is
         * already being downloaded.
         */
        FILE_ALREADY_DOWNLOADING,
        /** 
         * Thrown when the directory to save in already exceeds the maximum path name
         * on the OS.
         */
        PATH_NAME_TOO_LONG,
        
        /**
         * Thrown when trying to open a torrent file that is too large.
         */
        TORRENT_FILE_TOO_LARGE
        
    }
    
    /**
	 * The error code of this exception.
	 */
	private final LocationCode errorCode;
	/**
	 * Handle to the file that caused the exception.
	 */
	private final File file;
	
	/**
	 * Constructs a SaveLocationException with the specified cause and file.
	 */
	public SaveLocationException(IOException cause, File file) {
	    super(cause);
	    this.errorCode = LocationCode.FILESYSTEM_ERROR;
	    this.file = file;
	}
	
	public SaveLocationException(LocationCode errorCode, File file) {
		super("error code " + errorCode + ", file " + file);
		this.errorCode = errorCode;
		this.file = file;
	}
	
	/**
	 * Constructs a SaveLocationException for the specified error code.
	 * @param errorCode
	 * @param message optional more detailed message for debugging purposes
	 */
	public SaveLocationException(LocationCode errorCode, File file, String message) {
		super(message);
		this.errorCode = errorCode;
		this.file = file;
	}
	
	/**
	 * Returns the error code of this exception.
	 * @return
	 */
	public LocationCode getErrorCode() {
		return errorCode;
	}
	
	public File getFile() {
		return file;
	}
		
}
