package com.limegroup.gnutella.gui.dnd;

import java.io.File;

/**
 * An interface representing a file that can be transfered.
 */
public interface FileTransfer {
    /**
     * Retrieve the file for transfer.
     *
     * Null means no file availabe.  A folder means
     * all sub-files available.  A file means that file only.
     */
    public File getFile();
}