package org.limewire.http.httpclient;

/**
 * Default implementation of {@link HttpClientInstanceUtils}.
 */
public class DefaultHttpClientInstanceUtils implements HttpClientInstanceUtils {

    /**
     * Does nothing to the base url.
     */
    @Override
    public String addClientInfoToUrl(String baseUrl) {
        return baseUrl;
    }

}
