package com.limegroup.gnutella.downloader;

/**
 * Thrown when overlapped download bytes mismatch, i.e., bytes written to disk
 * don't match non-zero bytes already there.
 */
public class OverlapMismatchException extends Exception {
}
