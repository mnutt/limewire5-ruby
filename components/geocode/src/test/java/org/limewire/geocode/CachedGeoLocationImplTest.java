package org.limewire.geocode;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.geocode.GeocodeInformation.Property;
import org.limewire.inject.MutableProvider;
import org.limewire.inject.Providers;
import org.limewire.io.NetworkUtils;
import org.limewire.util.AssignParameterAction;

import com.google.inject.Provider;

import junit.framework.TestCase;

public class CachedGeoLocationImplTest extends TestCase {

    private Mockery context;
    private Geocoder geocoder;
    private CachedGeoLocationImpl geoLocation;
    private MutableProvider<Properties> geoLocationSetting;
    private AddressProvider addressProvider;

    public CachedGeoLocationImplTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        geocoder = context.mock(Geocoder.class);
        geoLocationSetting = context.mock(MutableProvider.class);
        addressProvider = new AddressProvider();
        geoLocation = new CachedGeoLocationImpl(geoLocationSetting, Providers.of(geocoder), addressProvider);
    }
    
    public void testGetGeocodeInformationWillTriggerInitializeOnlyOnce() {
        context.checking(new Expectations() {{
            one(geocoder).initialize();
            one(geocoder).getGeocodeInformation();
            will(returnValue(null));
            allowing(geoLocationSetting).get();
            will(returnValue(new Properties()));
        }});
        
        // called twice
        assertNull(geoLocation.get());
        assertNull(geoLocation.get());
        
        context.assertIsSatisfied();
    }
    
    public void testGetGeocodeInformationWillUseValueFromSetting() {
        addressProvider.address = new byte[] { (byte)129, 0, 0, 1 };
        GeocodeInformation info = new GeocodeInformation();
        info.setProperty(Property.Ip, NetworkUtils.ip2string(addressProvider.address));
        final Properties properties = new Properties();
        properties.putAll(info.toProperties());
        context.checking(new Expectations() {{
            never(geocoder).initialize();
            never(geocoder).getGeocodeInformation();
            allowing(geoLocationSetting).get();
            will(returnValue(properties));
        }});
        
        GeocodeInformation result = geoLocation.get();
        assertEquals(info.getProperty(Property.Ip), result.getProperty(Property.Ip));
        
        context.assertIsSatisfied();
    }

    public void testInitializeSavesNewValueToSetting() {
        final AtomicReference<Properties> properties = new AtomicReference<Properties>(null);
        final GeocodeInformation info = new GeocodeInformation();
        info.setProperty(Property.Ip, "129.0.0.1");
        context.checking(new Expectations() {{
            one(geocoder).initialize();
            one(geocoder).getGeocodeInformation();
            will(returnValue(info));
            allowing(geoLocationSetting).get();
            will(returnValue(new Properties()));
            allowing(geoLocationSetting).set(with(any(Properties.class)));
            will(new AssignParameterAction<Properties>(properties, 0));
        }});
        
        geoLocationSetting.set(new Properties());
        GeocodeInformation result = geoLocation.get();
        assertSame(info, result);
        
        assertEquals("129.0.0.1", properties.get().getProperty(Property.Ip.getValue()));
    }
    
    private static class AddressProvider implements Provider<byte []> {
        
        byte [] address;
        
        @Override
        public byte[] get() {
            return address;
        }
    }
}
