package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Basically just a renamed MalformedURLException.
 */

public class BadURLException extends IOException {
	public BadURLException() { super("Bad URL"); }
	public BadURLException(String msg) { super(msg); }
}
