package com.limegroup.gnutella.downloader;

/**
 * Callback for whenever this uploader starts or finishes to serve
 * an http11 request.
 */
interface HTTP11Listener {
    public void thexRequestStarted();
    public void thexRequestHandled();
    public void requestStarted(TestUploader uploader);
    public void requestHandled();
}