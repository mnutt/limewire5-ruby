package com.limegroup.gnutella.licenses;

import org.limewire.http.httpclient.LimeHttpClient;

class StubWeedLicense extends WeedLicense {
    
    private final String page;
    
    StubWeedLicense(String page) {
        this("1", "2", page);
    }
        
    StubWeedLicense(String cid, String vid, String page) {
        super(buildURI(cid, vid));
        this.page = page;
    }
    
    @Override
    protected String getBodyFromURL(String url, LimeHttpClient httpClient) {
        return page;
    }
    
}