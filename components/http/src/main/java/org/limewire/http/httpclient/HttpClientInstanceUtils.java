package org.limewire.http.httpclient;

public interface HttpClientInstanceUtils {
    /**
     * Adds client specific information in the query part to the base url. 
     */
    public String addClientInfoToUrl(String baseUrl);
}
