package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;

/** An observer for URNs. */
public interface ContentResponseObserver {
    /**
     * Called when a content response was received or the request timed out. 
     * @param urn the urn that was requested
     * @param response can be null due to a timeout
     */
    public void handleResponse(URN urn, ContentResponseData response);
}
