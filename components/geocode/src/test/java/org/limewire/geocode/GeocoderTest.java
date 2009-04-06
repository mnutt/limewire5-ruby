package org.limewire.geocode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.inject.Providers;
import org.limewire.util.BaseTestCase;

import com.google.inject.Provider;

import junit.framework.Test;

public class GeocoderTest extends BaseTestCase {
    private Mockery mockery;
    private LimeHttpClient httpClient;
    private Provider<String> geoCodeURL;
    private String testS;

    public GeocoderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(GeocoderTest.class);
    }
    
    private Geocoder geo;
    private Map<String,String> props;

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        
        // Build the properties
        props = new HashMap<String,String>();
        props.put("CountryCode","US");
        props.put("CountryCode3","USA");
        props.put("CountryName","United States");
        props.put("Region","NY");
        props.put("Region2","New York");
        props.put("City","New York");
        props.put("PostalCode","10004");
        props.put("Latitude","40.6888");
        props.put("Longitude","74.0203");
        props.put("DmaCode","501");
        props.put("AreaCode","212");
        
        // Build the String
        final String SEPARATOR = "\t";
        final String NEWLINE = "\n";
        String testString = SEPARATOR + NEWLINE;
        for (Map.Entry<String,String> en : props.entrySet()) {
            testString += en.getKey() + SEPARATOR + en.getValue() + NEWLINE;
        }
        testS = testString;

        mockery = new Mockery();
        httpClient = mockery.mock(LimeHttpClient.class);
        geoCodeURL = mockery.mock(Provider.class);
        geo = new GeocoderImpl(geoCodeURL, Providers.of(httpClient));
    }
    
    public void testSimple() throws InterruptedException {
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
        mockery.checking(new Expectations() {{
            allowing(geoCodeURL).get();
            will(returnValue("http://foo.com"));
            try {
                allowing(httpClient).execute(with(any(HttpUriRequest.class)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            will(returnValue(response));
            allowing(response).getStatusLine();
            will(returnValue(statusLine));
            allowing(statusLine).getStatusCode();
            will(returnValue(HttpStatus.SC_OK));
            allowing(response).getEntity();
            will(returnValue(httpEntity));
            try {
                allowing(httpEntity).getContent();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            will(returnValue(new StringInputStream(testS)));
            allowing(httpEntity).getContentType();
            will(returnValue(new BasicHeader("Content-Type", HTTP.DEFAULT_CONTENT_TYPE)));
            allowing(httpClient).releaseConnection(with(same(response)));
        }});
        // Test some state information
        assertFalse(geo.isReady());
        geo.initialize();
        assertTrue(geo.isReady());
        assertFalse(geo.hasFailed());
        GeocodeInformation info = geo.getGeocodeInformation();
        
        // Make sure we have all the correct properties
        for (GeocodeInformation.Property p : GeocodeInformation.getStrings2Properties().values()) {
            assertEquals(p.getValue() + " should be in " + info, props.get(p.getValue()), info.getProperty(p));
        }
    }
    
    public void testWantNull() {
        GeocodeInformation info = geo.getGeocodeInformation();
        assertNull(info);
        assertFalse("geo.isReady()", geo.isReady());
        assertFalse("geo.hasFailed()", geo.hasFailed());
    }
    
    private final class StringInputStream extends InputStream {
        private final String buf;
        private int cur;
        StringInputStream(String buf) {
            this.buf = buf;
        }
        @Override
        public int read() throws IOException {
            if (cur < buf.length()) return buf.charAt(cur++);
            return -1;
        }
    }
    
}
