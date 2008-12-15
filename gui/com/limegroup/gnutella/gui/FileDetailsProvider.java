package com.limegroup.gnutella.gui;

import com.limegroup.gnutella.FileDetails;

/**
 * Provides an array of file details which are currently selected.
 */
public interface FileDetailsProvider {

	FileDetails[] getFileDetails();
}
