package com.limegroup.gnutella.licenses;

import java.net.URI;

import org.limewire.http.httpclient.LimeHttpClient;

class StubCCLicense extends CCLicense {

    private static final URI LICENSE_URI;
    
    static {
        LICENSE_URI = URI.create("http://1.2.3.4/page");   
    }    
    
    private final String page;
    
    private final String details;
    
    StubCCLicense(String page) {
        this("license text", page);
    }

    StubCCLicense(String license, String page) {
        this(license, page, null);
    }
    
    StubCCLicense(String license, String page, String details) {
        super(license, getURI(license));
        this.page = page;
        this.details = details;
    }
   
    private static URI getURI(String license) {
        URI uri = LicenseFactoryImpl.getLicenseURI(license);
        if(uri == null)
            return LICENSE_URI;
        else
            return uri;
    }
    
    @Override
    protected String getBodyFromURL(String url, LimeHttpClient httpClient) {
        if(url.equals(getLicenseURI().toString()))
            return page;
        else
            return details;
    }
    
}