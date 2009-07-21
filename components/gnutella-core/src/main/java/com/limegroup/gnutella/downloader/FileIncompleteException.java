package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown id the downloaded file is incomplete.
 */
public class FileIncompleteException extends IOException {
    public FileIncompleteException() { super("File Incomplete"); }
    public FileIncompleteException(String msg) { super(msg); }
}

