package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 *  Thrown when the Incomplete Directory is null.
 */

public class NullIncompleteDirectoryException extends IOException {
    public NullIncompleteDirectoryException() {
        super("Incomplete Directory Is Null"); }
    public NullIncompleteDirectoryException(String msg) {
        super(msg); }
}
