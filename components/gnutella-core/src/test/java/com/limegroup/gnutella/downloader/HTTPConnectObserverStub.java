package com.limegroup.gnutella.downloader;

import java.net.Socket;

import com.limegroup.gnutella.downloader.HTTPConnectObserver;

public class HTTPConnectObserverStub extends HTTPConnectObserver {

    @Override
    public void handleConnect(Socket socket) {
    }

    public void shutdown() {
    }

}
