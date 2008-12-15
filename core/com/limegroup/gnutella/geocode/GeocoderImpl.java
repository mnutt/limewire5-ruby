package com.limegroup.gnutella.geocode;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.limewire.core.settings.GeocodeSettings;
import org.limewire.geocode.AbstractGeocoder;
import org.limewire.http.httpclient.LimeHttpClient;

import com.google.inject.Inject;
import com.google.inject.Provider;

final class GeocoderImpl extends AbstractGeocoder {
    
  //  private static final Log LOG = LogFactory.getLog(GeocoderImpl.class);
    
    private final Provider<LimeHttpClient> httpClient;
    
    @Inject
    public GeocoderImpl(Provider<LimeHttpClient> client) {
        this.httpClient = client;
    }

    public void initialize() {
        String url = GeocodeSettings.GEOCODE_URL.getValue();
        if(url == null || url.equals("")) {
            setInvalid(new IllegalArgumentException("No URL"));
            return;
        }
        
        HttpGet get = new HttpGet(url);        
        LimeHttpClient client = httpClient.get();
        HttpResponse response = null;
        try {
            //System.out.println("GeocodeImpl.initialize: calling client.execute");
            // TODO: The following call seems to hang on some systems.
            response = client.execute(get);
            //System.out.println("GeocodeImpl.initialize: response = " + response);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if(entity != null) {
                    setGeocodeInformation(entity.getContent());
                    return;
                }
            }            
            setInvalid(new IOException("invalid response"));
        } catch (IOException e) {
            setInvalid(e);
        } finally {
            client.releaseConnection(response);
        }
    }

}
