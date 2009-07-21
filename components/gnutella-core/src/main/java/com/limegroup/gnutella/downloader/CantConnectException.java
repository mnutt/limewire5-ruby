package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 *  Basically just a renamed ConnectException.
 */

public class CantConnectException extends IOException {
    public CantConnectException() { super("Can't Connect"); }
    public CantConnectException(String msg) { super(msg); }
}
