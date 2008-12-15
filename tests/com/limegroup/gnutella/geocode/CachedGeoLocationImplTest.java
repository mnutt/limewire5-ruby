package com.limegroup.gnutella.geocode;

import java.util.Properties;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.GeocodeSettings;
import org.limewire.geocode.GeocodeInformation;
import org.limewire.geocode.Geocoder;
import org.limewire.geocode.GeocodeInformation.Property;
import org.limewire.inject.Providers;
import org.limewire.io.NetworkUtils;

import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class CachedGeoLocationImplTest extends LimeTestCase {

    private Mockery context;
    private NetworkManagerStub networkManagerStub;
    private Geocoder geocoder;
    private CachedGeoLocationImpl geoLocation;

    public CachedGeoLocationImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CachedGeoLocationImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        networkManagerStub = new NetworkManagerStub();
        geocoder = context.mock(Geocoder.class);
        geoLocation = new CachedGeoLocationImpl(Providers.of(geocoder), networkManagerStub);
    }
    
    public void testGetGeocodeInformationWillTriggerInitializeOnlyOnce() {
        context.checking(new Expectations() {{
            one(geocoder).initialize();
            one(geocoder).getGeocodeInformation();
            will(returnValue(null));
        }});
        
        // called twice
        assertNull(geoLocation.getGeocodeInformation());
        assertNull(geoLocation.getGeocodeInformation());
        
        context.assertIsSatisfied();
    }
    
    public void testGetGeocodeInformationWillUseValueFromSetting() {
        networkManagerStub.setExternalAddress(new byte[] { (byte)129, 0, 0, 1 });
        context.checking(new Expectations() {{
            never(geocoder).initialize();
            never(geocoder).getGeocodeInformation();
        }});
        
        GeocodeInformation info = new GeocodeInformation();
        info.setProperty(Property.Ip, NetworkUtils.ip2string(networkManagerStub.getExternalAddress()));
        GeocodeSettings.GEO_LOCATION.setValue(info.toProperties());
        
        GeocodeInformation result = geoLocation.getGeocodeInformation();
        assertEquals(info.getProperty(Property.Ip), result.getProperty(Property.Ip));
        
        context.assertIsSatisfied();
    }

    public void testInitializeSavesNewValueToSetting() {
        final GeocodeInformation info = new GeocodeInformation();
        info.setProperty(Property.Ip, "129.0.0.1");
        context.checking(new Expectations() {{
            one(geocoder).initialize();
            one(geocoder).getGeocodeInformation();
            will(returnValue(info));
        }});
        
        GeocodeSettings.GEO_LOCATION.setValue(new Properties());
        
        GeocodeInformation result = geoLocation.getGeocodeInformation();
        assertSame(info, result);
        
        Properties props = GeocodeSettings.GEO_LOCATION.getValue();
        assertEquals("129.0.0.1", props.getProperty(Property.Ip.getValue()));
    }
}
