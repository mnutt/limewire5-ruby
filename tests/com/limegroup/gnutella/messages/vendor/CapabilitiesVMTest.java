package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;
import org.limewire.io.GUID;
import org.limewire.net.TLSManager;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;

public class CapabilitiesVMTest extends BaseTestCase {

    private CapabilitiesVMFactory factory;
    private MessageFactory messageFactory;
    private TLSManager tlsManager;

    public CapabilitiesVMTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CapabilitiesVMTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).to(DHTManagerStub.class);
            }            
        });
        factory = injector.getInstance(CapabilitiesVMFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        tlsManager = injector.getInstance(TLSManager.class);
    }
    
    public void testStaticConstructor() throws Exception {
        CapabilitiesVM vmp = factory.getCapabilitiesVM();
        assertGreaterThan(0, vmp.supportsFeatureQueries());
        assertTrue(vmp.supportsWhatIsNew());
        assertGreaterThan(0, vmp.supportsCapability("WHAT".getBytes()));
        assertEquals(-1, vmp.supportsCapability("MDHT".getBytes()));
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vmp.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        CapabilitiesVM vmpRead = (CapabilitiesVM) messageFactory.read(bais, Network.TCP);
        assertEquals(vmp, vmpRead);

        assertGreaterThan(0, vmpRead.supportsFeatureQueries());
        assertTrue(vmpRead.supportsWhatIsNew());
        assertGreaterThan(0, vmpRead.supportsCapability("WHAT".getBytes()));
        assertEquals(-1, vmp.supportsCapability("MDHT".getBytes()));
    }
    
    public void testDHTCapability() throws Exception { 
        CapabilitiesVM vmp = factory.getCapabilitiesVM();
        assertEquals(-1, vmp.supportsCapability("MDHT".getBytes()));
        
        factory.updateCapabilities();
        vmp = factory.getCapabilitiesVM();
        assertGreaterThan(-1, vmp.isActiveDHTNode());
    }
    
    public void testTLSCapability() throws Exception {
        tlsManager.setIncomingTLSEnabled(false);
        CapabilitiesVM vmp = factory.getCapabilitiesVM();
        assertEquals(-1, vmp.supportsTLS());
        assertEquals(-1, vmp.supportsCapability("TLS!".getBytes()));
        
        tlsManager.setIncomingTLSEnabled(true);
        factory.updateCapabilities();
        vmp = factory.getCapabilitiesVM();
        assertEquals(1, vmp.supportsTLS());
        assertEquals(1, vmp.supportsCapability("TLS!".getBytes()));
    }

    public void testNetworkConstructor() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        ByteUtils.short2leb((short)4, baos);
        CapabilitiesVMImpl.writeCapability(baos, "SUSH".getBytes(), 10, false);
        CapabilitiesVMImpl.writeCapability(baos, "NEIL".getBytes(), 5, false);
        CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, false);
        CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, false);
        VendorMessage vm = new CapabilitiesVMImpl(guid, ttl, hops, 0, 
                                              baos.toByteArray(), Network.UNKNOWN);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        CapabilitiesVM vmp = (CapabilitiesVM) messageFactory.read(bais, Network.TCP);
        // make sure it supports everything we expect....
        assertEquals(10, vmp.supportsCapability("SUSH".getBytes()));
        assertEquals(5,  vmp.supportsCapability("NEIL".getBytes()));
        assertEquals(3,  vmp.supportsCapability("DAWG".getBytes()));
        assertEquals(-1, vmp.supportsFeatureQueries());
        assertFalse(vmp.supportsWhatIsNew());

        // now creat another one, mix up the blocks that are supported, and
        // make sure they are equal....
        baos = new ByteArrayOutputStream();
        ByteUtils.short2leb((short)3, baos);
        CapabilitiesVMImpl.writeCapability(baos, "NEIL".getBytes(), 5, false);
        CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, false);
        CapabilitiesVMImpl.writeCapability(baos, "SUSH".getBytes(), 10, false);
        
        CapabilitiesVM vmpOther = 
            new CapabilitiesVMImpl(guid, ttl, hops, 0, baos.toByteArray(), Network.UNKNOWN);

        assertEquals(vmp, vmpOther);

    }

    public void testBadCases() throws Exception {
        ByteArrayOutputStream baos;
        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        try {
            // test missing info....
            baos = new ByteArrayOutputStream();
            ByteUtils.short2leb((short)4, baos);
            CapabilitiesVMImpl.writeCapability(baos, "NEIL".getBytes(), 5, false);
            CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, false);
            CapabilitiesVMImpl.writeCapability(baos, "SUSH".getBytes(), 10, false);
            new CapabilitiesVMImpl(guid, ttl, hops, 0, baos.toByteArray(), Network.UNKNOWN);
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }
        try {
            // test corrupt info....
            baos = new ByteArrayOutputStream();
            ByteUtils.short2leb((short)4, baos);
            CapabilitiesVMImpl.writeCapability(baos, "SUSH".getBytes(), 10, false);
            CapabilitiesVMImpl.writeCapability(baos, "NEIL".getBytes(), 5, false);
            CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, false);
            baos.write("crap".getBytes());
            new CapabilitiesVMImpl(guid, ttl, hops, 0, 
                                                   baos.toByteArray(), Network.UNKNOWN);
            fail("bpe should have been thrown.");
        } catch (BadPacketException expected) {
        }
    }
    
    public void testLargeVersions() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        
        ByteUtils.short2leb((short)1, baos);
        CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, false);
        ByteUtils.short2leb((short)3, baos);        
        CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, true);
        CapabilitiesVMImpl.writeCapability(baos, "SUSH".getBytes(), 10, true);
        CapabilitiesVMImpl.writeCapability(baos, "NEIL".getBytes(), 5, true);
        CapabilitiesVM vmpOther = 
            new CapabilitiesVMImpl(guid, ttl, hops, 0, baos.toByteArray(), Network.UNKNOWN);
        baos = new ByteArrayOutputStream();
        
        ByteUtils.short2leb((short)3, baos);
        CapabilitiesVMImpl.writeCapability(baos, "NEIL".getBytes(), 5, false);
        CapabilitiesVMImpl.writeCapability(baos, "DAWG".getBytes(), 3, false);
        CapabilitiesVMImpl.writeCapability(baos, "SUSH".getBytes(), 10, false);
        CapabilitiesVM vmpOneOther = 
            new CapabilitiesVMImpl(guid, ttl, hops, 0, baos.toByteArray(), Network.UNKNOWN);
        assertEquals(vmpOther,vmpOneOther);
    }
}
