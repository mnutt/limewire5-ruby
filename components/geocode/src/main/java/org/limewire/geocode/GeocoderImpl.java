package org.limewire.geocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.limewire.http.httpclient.LimeHttpClient;

import com.google.inject.Inject;
import com.google.inject.Provider;

final class GeocoderImpl implements Geocoder {
    
  //  private static final Log LOG = LogFactory.getLog(GeocoderImpl.class);

    private final Provider<String> geoCodeURL;
    private final Provider<LimeHttpClient> httpClient;
    private GeocodeInformation info;
    private boolean failed;

    @Inject
    public GeocoderImpl(@GeocodeUrl Provider<String> geoCodeURL,
                        Provider<LimeHttpClient> client) {
        this.geoCodeURL = geoCodeURL;
        this.httpClient = client;
    }

    public void initialize() {
        String url = geoCodeURL.get();
        if(url == null || url.equals("")) {
            failed = true;
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
                    String charset = EntityUtils.getContentCharSet(entity);
                    setGeocodeInformation(entity.getContent(), charset != null ? charset : HTTP.DEFAULT_CONTENT_CHARSET);
                    return;
                }
            }            
            failed = true;
        } catch (IOException e) {
            failed = true;
        } finally {
            client.releaseConnection(response);
        }
    }

    public GeocodeInformation getGeocodeInformation() {
        return info;
    }

    public boolean isReady() {
        return hasFailed() || info != null;
    }

    public boolean hasFailed() {
        return failed;
    }

    public void clear() {
        info = null;
    }

    /**
     * Read the lines and set the fields appropriately. The fields will be
     * name/value pairs separated by tabs (i.e. <code>\t</code>). The name
     * correspond to the set method on {@link GeocodeInformationImpl}.
     * <p>
     * For example: <code>countryName   United States</code> would cause a call
     * to <code>g.setCountryName("United States").
     * 
     * @param is input lines of the form
     * <blockquote>
     * &lt;first line ignored&gt;<br>
     * ( String[<em>Name</em>] &lt;tab&gt; String[<em>Value</em>] &lt;newline&gt; )<br>
     * [ repeat name/value pairs ]
     * </blockquote>
     * @param charset
     * @throws java.io.IOException 
     */
    protected void setGeocodeInformation(InputStream is, String charset) throws IOException {

        GeocodeInformation res = new GeocodeInformation();

        String separator = "\t";

        BufferedReader in = new BufferedReader(new InputStreamReader(is, charset));
        in.readLine(); // ignore the first line
        
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("") || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(separator);
            if (parts.length < 2) {
                continue;
            }
            String name = parts[0];
            String value = parts[1];
            res.setProperty(name, value);
        }
        this.info = res;
    }
}
