package org.limewire.mojito.routing.impl;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.NetworkSettings;

public class RemoteContactTest extends MojitoTestCase {
    
    public RemoteContactTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(RemoteContactTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testFixSourceAndContactAddress() {
        setLocalIsPrivate(false);
        
        // External Port == 0 -> force firewalled if it isn't!
        // even w/o calling fix..
        RemoteContact node1 = (RemoteContact)ContactFactory.createLiveContact(
                null, Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("localhost", 0), 0, Contact.DEFAULT_FLAG);
        
        assertNull(node1.getSourceAddress());
        assertTrue(node1.isFirewalled());
        
        node1.fixSourceAndContactAddress(new InetSocketAddress("localhost", 1024));
        
        assertNotNull(node1.getSourceAddress());
        assertTrue(node1.isFirewalled());
        
        assertEquals(new InetSocketAddress("localhost", 1024), node1.getSourceAddress());
        assertEquals(new InetSocketAddress("localhost", 1024), node1.getContactAddress());
        
        // External Port != 0 -> Contact Address = Source Address + external Port
        RemoteContact node2 = (RemoteContact)ContactFactory.createLiveContact(
                null, Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("dell.com", 2048), 0, Contact.DEFAULT_FLAG);
        
        assertNull(node2.getSourceAddress());
        assertFalse(node2.isFirewalled());
        
        // Cannot switch from a PUBLIC to a PRIVATE address
        node2.fixSourceAndContactAddress(new InetSocketAddress("localhost", 1024));
        
        assertNotNull(node2.getSourceAddress());
        assertFalse(node2.isFirewalled());
        
        assertEquals(new InetSocketAddress("localhost", 1024), node2.getSourceAddress());
        //assertEquals(new InetSocketAddress("localhost", 2048), node2.getContactAddress());
    }
    
    public void testUpdateWithExistingContact() {
        setLocalIsPrivate(false);
        
        KUID nodeId = KUID.createRandomID();
        
        RemoteContact node1 = (RemoteContact)ContactFactory.createLiveContact(
                new InetSocketAddress("localhost", 1024), 
                Vendor.UNKNOWN, Version.ZERO, nodeId, 
                new InetSocketAddress("localhost", 2048), 
                0, Contact.DEFAULT_FLAG);
        
        node1.setTimeStamp(100);
        node1.setRoundTripTime(100);
        node1.handleFailure();
        
        RemoteContact node2 = (RemoteContact)ContactFactory.createLiveContact(
                new InetSocketAddress("localhost", 1024), 
                Vendor.UNKNOWN, Version.ZERO, nodeId, 
                new InetSocketAddress("localhost", 2048), 
                0, Contact.DEFAULT_FLAG);
        
        node2.setTimeStamp(99); // Node #2 is elder than Node #1
        
        assertEquals(-1, node2.getRoundTripTime());
        assertEquals(0, node2.getFailures());
        assertEquals(0, node2.getLastFailedTime());
        assertTrue(node2.getTimeStamp() > 0 && node2.getTimeStamp() < Long.MAX_VALUE);
        
        node2.updateWithExistingContact(node1);
        
        assertEquals(100, node2.getRoundTripTime());
        assertEquals(1, node2.getFailures());
        assertEquals(node1.getLastFailedTime(), node2.getLastFailedTime());
        assertEquals(node1.getTimeStamp(), node2.getTimeStamp());
        
        // Make sure update works only with Contacts that have the same Node ID
        try {
            RemoteContact node4 = (RemoteContact)ContactFactory.createLiveContact(
                    new InetSocketAddress("localhost", 1024), 
                    Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 2048), 
                    0, Contact.DEFAULT_FLAG);
            
            node4.updateWithExistingContact(node2);
            fail(node4 + " should have rejected the exsting Node " + node2);
        } catch (IllegalArgumentException ignore) {}
    }
    
    public void testVendorAndVersion() {
        setLocalIsPrivate(false);
        
        Contact node1 = ContactFactory.createLocalContact(Vendor.valueOf(1), Version.valueOf(2), false);
        
        assertEquals(Vendor.valueOf(1), node1.getVendor());
        assertEquals(Version.valueOf(2), node1.getVersion());
        
        Contact node2 = ContactFactory.createLiveContact(
                new InetSocketAddress("localhost", 1024), 
                Vendor.valueOf(3), Version.valueOf(4), KUID.createRandomID(), 
                new InetSocketAddress("localhost", 2048), 
                0, Contact.DEFAULT_FLAG);
        
        assertEquals(Vendor.valueOf(3), node2.getVendor());
        assertEquals(Version.valueOf(4), node2.getVersion());
        
        Contact node3 = ContactFactory.createUnknownContact(
                Vendor.valueOf(5), Version.valueOf(6), KUID.createRandomID(), 
                new InetSocketAddress("localhost", 2048));
        
        assertEquals(Vendor.valueOf(5), node3.getVendor());
        assertEquals(Version.valueOf(6), node3.getVersion());
    }
    
    public void testAdaptiveTimeout() {
        setLocalIsPrivate(false);
        
        // This test relies on the default values
        NetworkSettings.DEFAULT_TIMEOUT.revertToDefault();
        NetworkSettings.MIN_TIMEOUT_RTT.revertToDefault();
        NetworkSettings.MIN_TIMEOUT_RTT_FACTOR.revertToDefault();
        
        Contact node1 = ContactFactory.createLiveContact(
                new InetSocketAddress("localhost", 1024), 
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("localhost", 2048), 
                0, Contact.DEFAULT_FLAG);
        
        assertEquals(-1L, node1.getRoundTripTime());
        assertEquals(NetworkSettings.DEFAULT_TIMEOUT.getValue(), node1.getAdaptativeTimeout());
        
        node1.setRoundTripTime(NetworkSettings.MIN_TIMEOUT_RTT.getValue() - 500L);
        assertEquals(NetworkSettings.MIN_TIMEOUT_RTT.getValue(), node1.getAdaptativeTimeout());
        
        node1.setRoundTripTime(NetworkSettings.MIN_TIMEOUT_RTT.getValue() + 500L);
        assertGreaterThan(NetworkSettings.MIN_TIMEOUT_RTT.getValue(), node1.getAdaptativeTimeout());
        
        Contact node2 = ContactFactory.createUnknownContact(
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("localhost", 2048));
        
        assertEquals(-1L, node2.getRoundTripTime());
        assertEquals(NetworkSettings.DEFAULT_TIMEOUT.getValue(), node2.getAdaptativeTimeout());
        
        node2.setRoundTripTime(NetworkSettings.MIN_TIMEOUT_RTT.getValue() - 500L);
        assertFalse(node2.isAlive());
        assertEquals(NetworkSettings.DEFAULT_TIMEOUT.getValue(), node2.getAdaptativeTimeout());
        
        ((RemoteContact)node2).alive();
        assertTrue(node2.isAlive());
        
        node2.setRoundTripTime(NetworkSettings.MIN_TIMEOUT_RTT.getValue() - 500L);
        assertEquals(NetworkSettings.MIN_TIMEOUT_RTT.getValue(), node2.getAdaptativeTimeout());
    }
    
    public void testPublicPrivateAddress() {
        setLocalIsPrivate(true);
        
        InetSocketAddress sourceAddress = new InetSocketAddress("localhost", 1234);
        InetSocketAddress contactAddress = new InetSocketAddress("216.244.101.15", 5000);
        InetSocketAddress externalAddress = new InetSocketAddress("216.244.101.16", 5000);
        
        RemoteContact node1 = (RemoteContact)ContactFactory
            .createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(), contactAddress, 0, Contact.DEFAULT_FLAG);
        
        // PUBLIC contact address and PRIVATE source address
        // Result: Don't use the IP from the source address
        node1.fixSourceAndContactAddress(sourceAddress);
        assertEquals(contactAddress, node1.getContactAddress());
        
        // PUBLIC contact address and PUBLIC source address
        // Result: Use the IP from the source address
        node1.fixSourceAndContactAddress(externalAddress);
        assertEquals(externalAddress, node1.getContactAddress());
        
        // As above...
        node1 = (RemoteContact)ContactFactory
            .createLiveContact(sourceAddress, Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(), contactAddress, 0, Contact.DEFAULT_FLAG);
        assertEquals(contactAddress, node1.getContactAddress());
        
        // As above...
        node1 = (RemoteContact)ContactFactory
            .createLiveContact(externalAddress, Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(), contactAddress, 0, Contact.DEFAULT_FLAG);
        assertEquals(externalAddress, node1.getContactAddress());
    }
}
