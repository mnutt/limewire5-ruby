package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.StringUtils;

import junit.framework.Test;

import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message.Network;

public class HeadPingTest extends LimeTestCase {
    
    public HeadPingTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HeadPingTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testVersion1PingIsNotPongGGEPCapable() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        out.write(StringUtils.toAsciiBytes(UrnHelper.SHA1.httpStringValue()));
        
        HeadPing ping = new HeadPing(new byte[16], (byte)0, (byte)0, 1, out.toByteArray(), Network.UNKNOWN);
        assertFalse(ping.isPongGGEPCapable());
    }
    
    public void testVersion2PingIsPongGGEPCapable() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        out.write(StringUtils.toAsciiBytes(UrnHelper.SHA1.httpStringValue()));
        
        HeadPing ping = new HeadPing(new byte[16], (byte)0, (byte)0, 2, out.toByteArray(), Network.UNKNOWN);
        assertTrue(ping.isPongGGEPCapable());
    }
    
    public void testCurrentVersionPingIsPongGGEPCapable() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        out.write(StringUtils.toAsciiBytes(UrnHelper.SHA1.httpStringValue()));
        
        HeadPing ping = new HeadPing(new byte[16], (byte)0, (byte)0, HeadPing.VERSION, out.toByteArray(), Network.UNKNOWN);
        assertTrue(ping.isPongGGEPCapable());
    }
    
    public void testVersionXPlus1PingIsStillPongGGEPCapable() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        out.write(StringUtils.toAsciiBytes(UrnHelper.SHA1.httpStringValue()));
        
        HeadPing ping = new HeadPing(new byte[16], (byte)0, (byte)0, HeadPing.VERSION+1, out.toByteArray(), Network.UNKNOWN);
        assertTrue(ping.isPongGGEPCapable());
    }

}
