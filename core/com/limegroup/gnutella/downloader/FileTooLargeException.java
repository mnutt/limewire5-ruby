package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown if too much has been downloaded, and the file is too big
 */
public class FileTooLargeException extends IOException {
	public FileTooLargeException() { super("File Too Large"); }
	public FileTooLargeException(String msg) { super(msg); }
}

