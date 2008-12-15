package com.limegroup.gnutella.gui.util;

import java.io.File;
import java.text.MessageFormat;

import org.limewire.core.api.download.SaveLocationException;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;

/**
 * Static helper class that handles exceptions from the core by creating 
 * localized error messages and presenting those to the user via
 * {@link MessageService}. 
 */
public class CoreExceptionHandler {

	/**
	 * Handles {@link SaveLocationException} by presenting an error dialog to
	 * the user.
	 * @param sle the exception to handle
	 * @throws IllegalArgumentException if the error code is not handled
	 */
	public static void handleSaveLocationError(SaveLocationException sle) {
		GUIMediator.showError(getSaveLocationErrorString(sle));
	}

	/**
	 * Returs a localized string summing up the details of the exception
	 * depending on its error code.
	 */
	public static String getShortSaveLocationErrorString(SaveLocationException sle) {
		switch (sle.getErrorCode()) {
		case SECURITY_VIOLATION:
			return I18n.tr
				("Invalid Filename");
		case FILE_ALREADY_SAVED:
			return I18n.tr
				("Download Already Finished");
		case DIRECTORY_NOT_WRITEABLE:
			return I18n.tr
				("No Write Permission");
		case DIRECTORY_DOES_NOT_EXIST:
			return I18n.tr
				("Folder Does Not Exist");
		case FILE_ALREADY_EXISTS:
			return I18n.tr
				("File Already Exists");
		case FILE_IS_ALREADY_DOWNLOADED_TO:
			return I18n.tr
				("Filename Taken");
		case NOT_A_DIRECTORY:
			return I18n.tr
				("Not a Folder");
		case FILE_NOT_REGULAR:
			return I18n.tr
				("Not a File");
		case FILESYSTEM_ERROR:
			return I18n.tr
				("Filesystem Error");
		case FILE_ALREADY_DOWNLOADING:
			return I18n.tr
				("Same File Already Being Downloaded");
		default:
			throw new IllegalArgumentException("Unhandled error code: " 
											   + sle.getErrorCode());
		}
	}
	
	/**
	 * Returns a localized string that explains in detail the exception
	 * depending on its error code.
	 */
	public static String getSaveLocationErrorString(SaveLocationException sle) {
        return getSaveLocationErrorString(sle, false);
    }
    
    /**
     * Returns a localized string that explains in detail the exception
     * depending on its error code.  If html is true, it will wrap any parameters
     * in italics.
     */
    public static String getSaveLocationErrorString(SaveLocationException sle, boolean html) {
		switch (sle.getErrorCode()) {
		case SECURITY_VIOLATION:
			return MessageFormat.format
				(I18n.tr("Could not set file location to {0}, because the filename is invalid and may corrupt your system."),
                        params(html, sle.getFile()));
		case FILE_ALREADY_SAVED:
			return I18n.tr("Could not set the file location, the download is already finished.");
		case DIRECTORY_NOT_WRITEABLE:
			return MessageFormat.format
			(I18n.tr("Could not save download to the folder {0}, because you do not have write access to it."),
                    params(html, sle.getFile()));
		case DIRECTORY_DOES_NOT_EXIST:
			return MessageFormat.format
				(I18n.tr("Could not save download to folder {0}, because the folder does not exist."),
                        params(html, sle.getFile()));
		case FILE_ALREADY_EXISTS:
			return MessageFormat.format
				(I18n.tr("A file already exists at {0}."),
                        params(html, sle.getFile()));
		case FILE_IS_ALREADY_DOWNLOADED_TO:
			return MessageFormat.format
				(I18n.tr("Another download is already saving to {0}."),
                        params(html, sle.getFile()));
		case NOT_A_DIRECTORY:
			return MessageFormat.format
			(I18n.tr("Could not set download folder to {0}, because it represents an individual file, not a folder."),
                    params(html, sle.getFile()));
		case FILE_NOT_REGULAR:
			return MessageFormat.format
				(I18n.tr("Could not set file location to {0}, because it is not a regular file."),
                        params(html, sle.getFile()));
		case FILESYSTEM_ERROR:
			return I18n.tr("Could not set file location due to an error in the filesystem.");
		case FILE_ALREADY_DOWNLOADING:
			return I18n.tr("The same file is already being downloaded.");
		case PATH_NAME_TOO_LONG:
		    return MessageFormat.format(I18n.tr("The path of the parent folder exceeds the maximum length of paths on the filesystem. {0} "),
		            params(html, sle.getFile()));
		default:
			throw new IllegalArgumentException("Unhandled error code: " 
											   + sle.getErrorCode());
		}
	}
    
    private static Object[] params(boolean html, File file) {
        String fileName = GUIUtils.convertToNonBreakingSpaces(0, file.toString());
        return new Object[] { html ? "<i>" + fileName + "</i>" : fileName };
    }
	
}
