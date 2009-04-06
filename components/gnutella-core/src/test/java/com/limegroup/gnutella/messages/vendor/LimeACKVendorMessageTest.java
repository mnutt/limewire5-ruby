package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;

public class LimeACKVendorMessageTest extends BaseTestCase {

    private SecurityToken token;
    private MessageFactory messageFactory;
    
    public LimeACKVendorMessageTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		messageFactory = injector.getInstance(MessageFactory.class);
        token = new AddressSecurityToken(InetAddress.getLocalHost(), 5904, injector.getInstance(MACCalculatorRepositoryManager.class));
    }
    
    public void testSecurityTokenBytesAreSet() {
        LimeACKVendorMessage msg = new LimeACKVendorMessage(new GUID(), 10, token);
        assertEquals(token.getBytes(), msg.getSecurityToken().getBytes());
    }
    
    public void testSecurityTokenBytesFromNetWork() throws BadPacketException {
        LimeACKVendorMessage in = new LimeACKVendorMessage(new GUID(), 10, token);
        LimeACKVendorMessage msg = new LimeACKVendorMessage(GUID.makeGuid(), (byte)1, (byte)1, 3, in.getPayload(), Network.UNKNOWN);
        assertEquals(token.getBytes(), msg.getSecurityToken().getBytes());
        assertEquals(10, msg.getNumResults());
    }
    
    public void testResultNum() {
        LimeACKVendorMessage msg = new LimeACKVendorMessage(new GUID(), 10, token);
        assertEquals(10, msg.getNumResults());
            
        for (int illegalNum : new int[] { 256, 0, -1 }) {
            try {
                msg = new LimeACKVendorMessage(new GUID(), illegalNum, token);
                fail("Expected IllegalArgumentException for " + illegalNum);
            }
            catch (IllegalArgumentException iae) {
            }
        }
    }

    public void testInvalidPayloadLengths() {
        for (int i = 0; i < 7; i++) {
            try {
                new LimeACKVendorMessage(GUID.makeGuid(), (byte)1, (byte)1, 3, new byte[i], Network.UNKNOWN);
                fail("payload is too short but no exception thrown");
            } catch (BadPacketException e) {
            }
        }
    }
    

    public void testLimeACK() throws Exception {
        try {
            GUID g = new GUID(GUID.makeGuid());
            new LimeACKVendorMessage(g, -1, token);
            assertTrue(false);
        } catch(IllegalArgumentException expected) {}
        try {
            GUID g = new GUID(GUID.makeGuid());
            new LimeACKVendorMessage(g, 256, token);
            assertTrue(false);
        } catch(IllegalArgumentException expected) {}

        for (int i = 1; i < 256; i++) {
            GUID guid = new GUID(GUID.makeGuid());
            LimeACKVendorMessage vm = new LimeACKVendorMessage(guid, i, token);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), i);
            assertEquals("guids aren't equal!", guid, new GUID(vm.getGUID()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            LimeACKVendorMessage vmRead = 
                (LimeACKVendorMessage) messageFactory.read(bais, Network.TCP);
            assertEquals(vm, vmRead);
            assertEquals("Read accessor is broken!", vmRead.getNumResults(), i);
            assertEquals("after Read guids aren't equal!", guid, 
                         new GUID(vmRead.getGUID()));
        }

        // test that the VM can be backwards compatible....
        byte[] payload = null;
        LimeACKVendorMessage vm = null;
        
        // first test that it needs a payload of at least size 1
        payload = new byte[0];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 0, payload, Network.UNKNOWN);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // first test that it rejects all versions of 1
        payload = new byte[1];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 1, payload, Network.UNKNOWN);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // first test that version 2 needs a payload of only size 1
        payload = new byte[2];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 2, payload, Network.UNKNOWN);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // test that it can handle versions other than 1
        payload = new byte[7];
        try {
            vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 3, payload, Network.UNKNOWN);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), 0);
        }
        catch (BadPacketException expected) {
            assertTrue(false);
        }
    }
    
}
