package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.GUID;

import junit.framework.Test;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;

/** Tests TCP/UDP ConnectBackVendorMessage
 */
public class ConnectBackVendorMessageTest extends com.limegroup.gnutella.util.LimeTestCase {

    private static final int UDP_VERSION = 
        UDPConnectBackVendorMessage.VERSION;

    private static final int TCP_VERSION = 
        TCPConnectBackVendorMessage.VERSION;

    public ConnectBackVendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ConnectBackVendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testUDPConnectBackConstructor() throws Exception {
        byte[] guid = GUID.makeGuid();
        UDPConnectBackVendorMessage udp = null;
        byte ttl = 1, hops = 0;
        
        try {
            // try a VERSION we don't support, with the now 2-byte payload
            udp = new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION+1, bytes(2), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        
        try {
            // try a VERSION we don't support, with the old 18-byte payload
            udp = new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION+1, bytes(18), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}

        try {
            // in the next few tests, try bad sizes of the payload....
            udp = new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION, bytes(0), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            udp = new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION, bytes(17), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            udp = new UDPConnectBackVendorMessage(guid, ttl, hops,
                                                UDP_VERSION, bytes(19), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}

        // Test version 1 constructor -- 18 bytes in payload
        udp = new UDPConnectBackVendorMessage(guid, ttl, hops, 1, bytes(18), Network.UNKNOWN);
        // no bpe ...
        
        // make sure we encode things just fine....
        GUID guidObj = new GUID(GUID.makeGuid());

        UDPConnectBackVendorMessage VendorMessage1 = 
            new UDPConnectBackVendorMessage(6346, guidObj);
        UDPConnectBackVendorMessage VendorMessage2 = 
            new UDPConnectBackVendorMessage(VendorMessage1.getGUID(), ttl, hops,
                                            VendorMessage1.getVersion(),
                                            VendorMessage1.getPayload(), Network.UNKNOWN);
        assertEquals(1, VendorMessage1.getVersion());
        assertEquals(VendorMessage2, VendorMessage1);
        assertEquals(VendorMessage1.getConnectBackPort(),
                     VendorMessage2.getConnectBackPort());
        assertEquals(VendorMessage1.getConnectBackGUID(),
                     VendorMessage2.getConnectBackGUID());

        //Test version 2 constructor -- 2 bytes in payload.
        udp = new UDPConnectBackVendorMessage(guid, ttl, hops, UDP_VERSION, 
                                              bytes(2), Network.UNKNOWN);
        assertEquals(2, udp.getVersion());
        assertEquals(udp.getConnectBackGUID(), new GUID(guid));
        assertEquals(1, udp.getConnectBackPort());
    }
    
    /**
     * Creates a byte array whose first byte is non zero.
     */
    private byte[] bytes(int length) {
        byte[] stuff = new byte[length];
        if( length > 0 )
            stuff[0] = 1;
        return stuff;
    }

    public void testTCPConnectBackConstructor() throws Exception {
        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        try {
            // try a VERSION we don't support
            new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION+1, bytes(2), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            // in the next few tests, try bad sizes of the payload....
            new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION, bytes(0), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION, bytes(1), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            new TCPConnectBackVendorMessage(guid, ttl, hops,
                                                TCP_VERSION, bytes(3), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}

        // this is the correct size of the payload
        new TCPConnectBackVendorMessage(guid, ttl, hops,
                                            TCP_VERSION, bytes(2), Network.UNKNOWN);


        // make sure we encode things just fine....
        TCPConnectBackVendorMessage VendorMessage1 = 
            new TCPConnectBackVendorMessage(6346);
        TCPConnectBackVendorMessage VendorMessage2 = 
            new TCPConnectBackVendorMessage(VendorMessage1.getGUID(),
                                            ttl, hops, TCP_VERSION, 
                                            VendorMessage1.getPayload(), Network.UNKNOWN);
        assertEquals(VendorMessage1, VendorMessage2);
        assertEquals(VendorMessage1.getConnectBackPort(),
                     VendorMessage2.getConnectBackPort());

    }


}
