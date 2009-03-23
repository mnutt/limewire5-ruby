package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;

public class StubContentResponseObserver implements ContentResponseObserver {

    private URN urn;
    private ContentResponseData response;
    
    public void handleResponse(URN urn, ContentResponseData response) {
        this.urn = urn;
        this.response = response;
    }

    public ContentResponseData getResponse() {
        return response;
    }

    public URN getUrn() {
        return urn;
    }

}
