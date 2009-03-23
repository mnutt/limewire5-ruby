package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when the ByteReader is null
 */

public class ReaderIsNullException extends IOException {
	public ReaderIsNullException() { super("Reader is Null"); }
	public ReaderIsNullException(String msg) { super(msg); }
}
