package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;

/** An observer for URNs. */
public interface ContentResponseObserver {
    public void handleResponse(URN urn, ContentResponseData response);
}
