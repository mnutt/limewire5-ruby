package com.limegroup.gnutella.altlocs;

import junit.framework.Test;

import org.limewire.collection.Function;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.Connectable;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressProviderStub;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.UrnHelper;

public class AltLocUtilsTest extends BaseTestCase {

    private AlternateLocationFactory alternateLocationFactory;
    private LocalSocketAddressProviderStub localSocketAddressProvider;


    public AltLocUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocUtilsTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        
        localSocketAddressProvider = new LocalSocketAddressProviderStub();
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalSocketAddressProvider.class).toInstance(localSocketAddressProvider);
            }
        });
        
        
        localSocketAddressProvider.setTLSCapable(true);
        localSocketAddressProvider.setLocalPort(6346);
        
        this.alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
    }
    
    public void testParseAlternateLocationsNoTLS() throws Exception {
        String data = "1.2.3.4,1.2.3.5,1.2.3.6,1.2.3.7,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, false, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, false); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, false); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, false); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "1.2.3.7", 6346, false); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, false); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }
    
    public void testParseAlternateLocationsNoTLSButAllowed() throws Exception {
        String data = "1.2.3.4,1.2.3.5,1.2.3.6,1.2.3.7,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, true, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, false); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, false); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, false); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "1.2.3.7", 6346, false); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, false); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }
    
    public void testParseAlternateLocationsTLSNotAllowed() throws Exception {
        String data = "tls=A8,1.2.3.4,1.2.3.5,1.2.3.6,1.2.3.7,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, false, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, false); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, false); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, false); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "1.2.3.7", 6346, false); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, false); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }
    
    public void testParseAlternateLocationsTLS() throws Exception {
        String data = "tls=A8,1.2.3.4,1.2.3.5,1.2.3.6,1.2.3.7,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, true, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, true); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, false); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, true); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "1.2.3.7", 6346, false); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, true); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }    
    
    public void testParseAlternateLocationsSkipsInvalid() throws Exception {
        String data = "abc,1.2.3.4,def,1.2.3.5,1.2.3.6,lalala,1.2.3.7,12.3.4.5.6.7:108,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, false, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, false); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, false); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, false); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "1.2.3.7", 6346, false); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, false); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }
    
    public void testParseAlternateLocationsInvalidTurnsOffTLS() throws Exception {
        String data = "tls=FFF,1.2.3.4,1.2.3.5,1.2.3.6,lalala,1.2.3.7,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, true, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, true); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, true); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, true); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "1.2.3.7", 6346, false); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, false); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }
    
    public void testParseAlternateLocationsSkips127001() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        // Note that EC == 111011, but only 4 get passed to the function.
        // TLS=EC is meant to apply to the whole set, including 127.0.0.1, but we filter it out
        // before handing it on.
        String data = "tls=EC,1.2.3.4,1.2.3.5,1.2.3.6,127.0.0.1,1.2.3.7,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, true, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, true); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, true); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, true); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "1.2.3.7", 6346, true); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, true); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }
    
    public void testParseAlternateLocationsSkipsLocalHost() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        localSocketAddressProvider.setLocalAddress(new byte[] { 8, 7, 1, 2 } );
        localSocketAddressProvider.setLocalPort(1234);
        // Note that EC == 111011, but only 4 get passed to the function.
        // TLS=EC is meant to apply to the whole set, including localhost, but we filter it out
        // before handing it on.
        String data = "tls=EC,1.2.3.4,1.2.3.5,1.2.3.6,8.7.1.2:1234,8.7.1.2:1,1.2.3.8:101";
        AltLocUtils.parseAlternateLocations(UrnHelper.SHA1, data, true, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            int i = 0;
            public Void apply(AlternateLocation argument) {
                switch(i++) {
                case 0: checkDirect(argument, UrnHelper.SHA1, "1.2.3.4", 6346, true); break;
                case 1: checkDirect(argument, UrnHelper.SHA1, "1.2.3.5", 6346, true); break;
                case 2: checkDirect(argument, UrnHelper.SHA1, "1.2.3.6", 6346, true); break;
                case 3: checkDirect(argument, UrnHelper.SHA1, "8.7.1.2", 1, true); break;
                case 4: checkDirect(argument, UrnHelper.SHA1, "1.2.3.8", 101, true); break;
                default: fail("invalid i: " + i + ", argument: " + argument);
                }
                return null;
            }
        });
    }
    
    private void checkDirect(AlternateLocation alt, URN sha1, String host, int port, boolean tls) {
        assertInstanceof(DirectAltLoc.class, alt);
        DirectAltLoc d = (DirectAltLoc)alt;
        assertEquals(sha1, d.getSHA1Urn());
        assertEquals(host, d.getHost().getAddress());
        assertEquals(port, d.getHost().getPort());
        if(tls) {
            assertInstanceof(Connectable.class, d.getHost());
            assertTrue(((Connectable)d.getHost()).isTLSCapable());
        } else {
            if(d.getHost() instanceof Connectable)
                assertFalse(((Connectable)d.getHost()).isTLSCapable());
        }
    }

}
