package org.limewire.security;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Test;

import org.limewire.concurrent.SimpleTimer;
import org.limewire.util.BaseTestCase;


public class AddressSecurityTokenTest extends BaseTestCase {
    private MACCalculatorRepositoryManager macManager = new MACCalculatorRepositoryManager();
    
    public AddressSecurityTokenTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AddressSecurityTokenTest.class);
    }

    public void testConstruction() throws Exception {
        try {
            new AddressSecurityToken(new byte[3],macManager);
            fail("exception should have been thrown.");
        }
        catch (InvalidSecurityTokenException ignored) {}
        try {
            new AddressSecurityToken(new byte[17],macManager);
            fail("exception should have been thrown.");
        }
        catch (InvalidSecurityTokenException ignored) {}
        
        byte[] qk = new byte[8];
        Random rand = new Random();
        Arrays.sort(qk);
        while (Arrays.binarySearch(qk, (byte) 0x1c) < 0) {
            rand.nextBytes(qk);
            Arrays.sort(qk);
        }
        AddressSecurityToken key1, key2;
        key1 = new AddressSecurityToken(qk,macManager);
        key2 = new AddressSecurityToken(qk,macManager);
        assertEquals(key1,key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        
        byte []qk3 = new byte[8];
        rand.nextBytes(qk3);
        AddressSecurityToken key3 = new AddressSecurityToken(qk3,macManager);
        assertNotEquals(key1, key3);
        assertNotEquals(key2, key3);
        assertEquals(key3, key3);
        // don't test key1.hashCode() vs. key3.hashCode() - they may very easily
        // conflict
    }

    public void testSimpleGeneration() throws Exception {
        InetAddress ip;
        ip = InetAddress.getByName("www.limewire.com");
        int port = 6346;
        AbstractSecurityToken qk1 = new AddressSecurityToken(ip, port,macManager);
        AbstractSecurityToken qk2 = new AddressSecurityToken(ip, port,macManager);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("10.254.0.42");
        qk1 = new AddressSecurityToken(ip, port,macManager);
        qk2 = new AddressSecurityToken(ip, port,macManager);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("127.0.0.1");
        qk1 = new AddressSecurityToken(ip, port,macManager);
        qk2 = new AddressSecurityToken(ip, port,macManager);
        assertEquals(qk1,qk2);
    }

    public void testQueryKeys() throws Exception {
        InetAddress addr1 = InetAddress.getByName("www.limewire.com");
        InetAddress addr2 = InetAddress.getByName("www.microsoft.com");
        
        int port1 = 1234;
        int port2 = 4321;
        
        AddressSecurityToken key1 = new AddressSecurityToken(addr1, port1,macManager);
        AddressSecurityToken key2 = new AddressSecurityToken(addr1, port2,macManager);
        AddressSecurityToken key3 = new AddressSecurityToken(addr2, port1,macManager);
        
        assertTrue(key1.isFor(addr1, port1));
        assertFalse(key1.equals(key2));
        assertFalse(key1.equals(key3));
        assertFalse(key1.isFor(addr1, port2));
        assertFalse(key1.isFor(addr2, port1));
        assertFalse(key1.isFor(addr2, port2));
        
        assertTrue(key2.isFor(addr1, port2));
        assertFalse(key2.equals(key1));
        assertFalse(key2.equals(key3));
        assertFalse(key2.isFor(addr1, port1));
        assertFalse(key2.isFor(addr2, port1));
        assertFalse(key2.isFor(addr2, port2));
        
        assertTrue(key3.isFor(addr2, port1));
        assertFalse(key3.equals(key1));
        assertFalse(key3.equals(key2));
        assertFalse(key3.isFor(addr1, port1));
        assertFalse(key3.isFor(addr1, port2));
        assertFalse(key3.isFor(addr2, port2));
    }
    
    public void testOddsAndEnds() throws Exception {
        // test to make clover 100% for QK
        AbstractSecurityToken qk = new AddressSecurityToken(InetAddress.getLocalHost(), 6346,macManager);
        assertFalse(qk.equals(new Object()));
        qk.toString();
    }
    
    public void testQueryKeyExpiration() throws Exception {
        NotifyingSettingsProvider settings = new NotifyingSettingsProvider();
        MACCalculatorRepositoryManager macManager2 = new MACCalculatorRepositoryManager(new SimpleTimer(true),settings);

        InetAddress address = InetAddress.getLocalHost();

        settings.notified = false;
        AddressSecurityToken key = new AddressSecurityToken(address, 4545, macManager2);

        // wait for secret key change, this relies on the implementation
        // detail that when the rotation is run the setting provider is queried
        // for its values, thus we can wake up when that happens
        settings.waitForRotation();

        // key should still be valid
        assertTrue(key.isFor(address, 4545));
        // different port
        assertFalse(key.isFor(address, 4544));

        // wait for grace period to be over
        Thread.sleep(250);

        assertFalse(key.isFor(address, 4545));

    }
    
    private static class NotifyingSettingsProvider implements SettingsProvider {

        boolean notified = false;
        
        public synchronized long getChangePeriod() {
            notified = true;
            notify();
            return 400;
        }

        public long getGracePeriod() {
            return 200;
        }
        
        public synchronized void waitForRotation() throws InterruptedException {
            while (!notified) {
                wait();
            }
        }
        
    }
    
}
