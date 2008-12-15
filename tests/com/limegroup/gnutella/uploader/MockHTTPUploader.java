/**
 * 
 */
package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.http.AltLocTracker;


class MockHTTPUploader extends HTTPUploader {
    private StubAltLocTracker tracker = new StubAltLocTracker();

    public MockHTTPUploader() {
        super(null, null);
    }

    @Override
    public AltLocTracker getAltLocTracker() {
        return tracker;
    }
}