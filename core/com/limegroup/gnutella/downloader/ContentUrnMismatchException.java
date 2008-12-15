package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * @author Gregorio Roper
 * 
 * Exception thrown when the X-Gnutella-Content-URN does not match the expected
 * sha1 urn
 */
public class ContentUrnMismatchException extends IOException {

    /**
     * Constructor
     */
    public ContentUrnMismatchException() {
        super("ContentUrnMismatch");
    }
}
