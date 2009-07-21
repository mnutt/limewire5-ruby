package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown if the file couldn't be moved to the Library.
 */
public class FileCantBeMovedException extends IOException {
    public FileCantBeMovedException() { super("File Couldn't Be Moved"); }
    public FileCantBeMovedException(String msg) { super(msg); }
}
