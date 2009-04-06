package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;

public class ReplyNumberVendorMessageTest extends BaseTestCase {

    private ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory;
    private MessageFactory messageFactory;
    private NetworkManagerStub networkManagerStub;

    public ReplyNumberVendorMessageTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        replyNumberVendorMessageFactory = injector.getInstance(ReplyNumberVendorMessageFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
    }
    
    public void testNumResults() {
        ReplyNumberVendorMessage msg = replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(), 10);
        assertEquals(10, msg.getNumResults());
        
        for (int illegalResultNum : new int[] { 256, 0, -1 }) {
            try { 
                msg = replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(), illegalResultNum);
                fail("Expected illegal argument exception for result: " + illegalResultNum);
            }
            catch (IllegalArgumentException iae) {
            }
        }
    }
    
    public void testVersion3AllowsLargerMessagesFromNetwork() throws BadPacketException {
        replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                (byte)1, (byte)1, 3, new byte[11], Network.UNKNOWN);
        try {
            replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                    (byte)1, (byte)1, 2, new byte[11], Network.UNKNOWN);
            fail("BadPacketException expected, message too large");
        }
        catch (BadPacketException e) {
        }
    }
    
    public void testOldVersionIsNotAcceptedFromNetwork() {
        try {
            replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                    (byte)0x01, (byte)0x01, 1, new byte[2], Network.UNKNOWN);
            fail("Old message versions should not be accpeted");            
        }
        catch (BadPacketException bpe) {
        }
    }
    
    public void testReplyNumber() throws Exception {
        try {
            GUID g = new GUID(GUID.makeGuid());
            replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(g, 0);
            assertTrue(false);
        } catch(IllegalArgumentException expected) {}
        try {
            GUID g = new GUID(GUID.makeGuid());
            replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(g, 256);
            assertTrue(false);
        } catch(IllegalArgumentException expected) {}

        for (int i = 1; i < 256; i++) {
            GUID guid = new GUID(GUID.makeGuid());
            ReplyNumberVendorMessage vm = replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(guid,
                                                                       i);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), i);
            assertEquals("guids aren't equal!", guid, new GUID(vm.getGUID()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            ReplyNumberVendorMessage vmRead = 
                (ReplyNumberVendorMessage) messageFactory.read(bais, Network.TCP);
            assertEquals(vm, vmRead);
            assertEquals("Read accessor is broken!", vmRead.getNumResults(), i);
            assertEquals("after Read guids aren't equal!", guid, 
                         new GUID(vmRead.getGUID()));
        }

        // test that the VM can be backwards compatible....
        byte[] payload = null;
        ReplyNumberVendorMessage vm = null;
        
        // first test that it needs a payload of at least size 1
        payload = new byte[0];
        try {
            vm = replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 0, payload, Network.UNKNOWN);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        // first test that version 1 needs a payload of only size 1
        payload = new byte[2];
        try {
            vm = replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 1, payload, Network.UNKNOWN);
            assertTrue(false);
        }
        catch (BadPacketException expected) {};

        //test that it can handle versions other than 1
        payload = new byte[3];
        try {
            vm = replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 3, payload, Network.UNKNOWN);
            assertEquals("Simple accessor is broken!", vm.getNumResults(), 0);
        }
        catch (BadPacketException expected) {
            assertTrue(false);
        }
        
        //test un/solicited byte
        networkManagerStub.setCanReceiveUnsolicited(false);
        
        vm = replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(GUID.makeGuid()),5);
        assertFalse(vm.canReceiveUnsolicited());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ReplyNumberVendorMessage vm2 = (ReplyNumberVendorMessage) messageFactory.read(bais, Network.TCP);
        assertFalse(vm2.canReceiveUnsolicited());
        
        networkManagerStub.setCanReceiveUnsolicited(true);
        
        vm = replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(GUID.makeGuid()),5);
        assertTrue(vm.canReceiveUnsolicited());
        
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        vm2 = (ReplyNumberVendorMessage) messageFactory.read(bais, Network.TCP);
        assertTrue(vm2.canReceiveUnsolicited());
        
        
    }



}
