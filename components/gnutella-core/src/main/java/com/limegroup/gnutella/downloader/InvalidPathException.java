package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when the download path is invalid.
 */
public class InvalidPathException extends IOException {
    public InvalidPathException() { super("Invalid Path"); }
    public InvalidPathException(String msg) { super(msg); }
}
