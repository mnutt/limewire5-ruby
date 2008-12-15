package org.limewire.io;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.Decorator;
/**
 * Tests certain features of NetworkUtils
 */
public class NetworkUtilsTest extends BaseTestCase {

    /**
     * Array of private addresses for testing.
     */
    private static final String[] PRIVATE_ADDRESSES = {
        "0.0.0.0",
        "0.0.0.1",
        "10.0.0.0",
        "10.254.0.1",
        "127.1.1.1",
        "127.0.0.0",
        "172.16.0.0",
        "172.31.255.255",
        "172.17.0.255",
        "192.168.0.0",
        "192.168.0.1",
        "192.168.255.255",
        "169.254.0.0",
        "169.254.255.255",
        "255.1.2.3",
        "255.0.0.0"
        //"240.0.0.0",
    };

    /**
     * Array of public addresses for testing.
     */
    private static final String[] PUBLIC_ADDRESSES = {
        "2.32.0.1",
        "20.43.2.1",
        "1.32.0.1",
        "172.15.0.1",
        "172.32.0.1",
        "192.167.0.1",
        "192.169.0.1",
        "1.0.0.0",
        "180.32.0.1",
        "239.32.0.1",
        "128.0.0.1",
        "169.253.0.0",
        "169.255.0.0",
    };

    public NetworkUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NetworkUtilsTest.class);
    }

    public void testIsDottedIPV4() throws Exception {
        for (String s : PUBLIC_ADDRESSES)
            assertTrue(NetworkUtils.isDottedIPV4(s));
        for (String s : PRIVATE_ADDRESSES)
            assertTrue(NetworkUtils.isDottedIPV4(s));
        
        assertFalse(NetworkUtils.isDottedIPV4("a.b.c.d"));
        assertFalse(NetworkUtils.isDottedIPV4("1.2.3"));
        assertFalse(NetworkUtils.isDottedIPV4("1.2.3."));
        assertFalse(NetworkUtils.isDottedIPV4("1.2.3.4."));
        assertFalse(NetworkUtils.isDottedIPV4("1.2.3.4 "));
        assertFalse(NetworkUtils.isDottedIPV4(".1.2.3.4"));
        assertFalse(NetworkUtils.isDottedIPV4(""));
        assertFalse(NetworkUtils.isDottedIPV4("1"));
        assertFalse(NetworkUtils.isDottedIPV4("."));
        assertFalse(NetworkUtils.isDottedIPV4("........"));
        assertFalse(NetworkUtils.isDottedIPV4("1.2.3.400"));
        assertFalse(NetworkUtils.isDottedIPV4("-1.2.3.4"));
    }
    
    /**
     * Tests the method for checking whether or not an IP address is
     * "close" to this address.
     */
    public void testIsCloseIP() throws Exception {
        byte[] addr0 = new byte[4];
        byte[] addr1 = new byte[4];

        addr0[0] = (byte)3;
        addr1[0] = (byte)3;
        addr0[1] = (byte)2;
        addr1[1] = (byte)2;
        assertTrue("should be considered close", 
                   NetworkUtils.isCloseIP(addr0, addr1));

        addr0[1] = (byte)4;
        assertTrue("should be considered close", 
                   NetworkUtils.isCloseIP(addr0, addr1));

        addr0[0] = (byte)0;
        assertTrue("should not be considered close", 
                   !NetworkUtils.isCloseIP(addr0, addr1));
    }
    
    public void testFilterUnique() throws Exception {
        List<IpPortImpl> l = new ArrayList<IpPortImpl>();
        l.add(new IpPortImpl("1.1.1.1",2));
        l.add(new IpPortImpl("1.1.1.2",2));
        l.add(new IpPortImpl("1.1.1.3",2));
        l.add(new IpPortImpl("1.1.2.3",2));
        
        Collection<IpPortImpl> ret = NetworkUtils.filterUnique(l, 0xFF000000);
        assertEquals(1, ret.size());
        assertEquals(1, NetworkUtils.filterUnique(l, 0xFFFF0000).size());
        assertEquals(2, NetworkUtils.filterUnique(l, 0xFFFFFF00).size());
        assertEquals(4, NetworkUtils.filterUnique(l, 0xFFFFFFFF).size());
    }

    /**
     * Tests the method for checking if something's from a private
     * network with a string argument.
     */
    public void testIsPrivateAddressWithString() throws Exception {
        for(int i=0; i<PUBLIC_ADDRESSES.length; i++) {
            String address = PUBLIC_ADDRESSES[i];
            assertTrue("should not be a private address: "+address, 
                       !NetworkUtils.isPrivateAddress(InetAddress.getByName(address)));
        }

        for(int i=0; i<PRIVATE_ADDRESSES.length; i++) {
            String address = PRIVATE_ADDRESSES[i];
            assertTrue("should be a private address: "+address, 
                       NetworkUtils.isPrivateAddress(InetAddress.getByName(address)));
        }
    }

    /**
     * Tests the method for whether or not an address is private,
     * with the address is bytes.
     */
    public void testIsPrivateAddress() throws Exception {
        for(int i=0; i<PUBLIC_ADDRESSES.length; i++) {
            InetAddress addr = InetAddress.getByName(PUBLIC_ADDRESSES[i]);
            
            // Check the InetAddress
            assertFalse("should be a public address"+addr,
                    NetworkUtils.isPrivateAddress(addr));
            
            // and the byte-array version
            assertFalse("should be a public address"+addr,
                    NetworkUtils.isPrivateAddress(addr.getAddress()));
            
        }

        for(int i=0; i<PRIVATE_ADDRESSES.length; i++) {
            InetAddress addr = InetAddress.getByName(PRIVATE_ADDRESSES[i]);
            
            // Check the InetAddress
            assertTrue("should be a private address" + addr,
                       NetworkUtils.isPrivateAddress(addr));
            
            // and the byte-array version
            assertTrue("should be a private address" + addr,
                    NetworkUtils.isPrivateAddress(addr.getAddress()));
        }
    }
    
    public void testIsInvalidAddressWithIP() throws Exception {
        IP ip = new IP("0.0.0.0");
        assertFalse(NetworkUtils.isValidAddress(ip));
        
        ip = new IP("0.0.0.1");
        assertFalse(NetworkUtils.isValidAddress(ip));
        
        ip = new IP("1.2.3.0");
        assertTrue(NetworkUtils.isValidAddress(ip));
        
        ip = new IP("255.0.1.1");
        assertFalse(NetworkUtils.isValidAddress(ip));
        
        ip = new IP("1.2.3.255");
        assertTrue(NetworkUtils.isValidAddress(ip));
    }

    public void testIsInvalidAddress() throws UnknownHostException {
        // A bunch of invalid addresses
        InetAddress addr1 = InetAddress.getByName("0.0.0.0");
        assertFalse(NetworkUtils.isValidAddress(addr1));
        assertFalse(NetworkUtils.isValidAddress(addr1.getAddress()));
        
        InetAddress addr2 = InetAddress.getByName("0.0.0.1");
        assertFalse(NetworkUtils.isValidAddress(addr2));
        assertFalse(NetworkUtils.isValidAddress(addr2.getAddress()));
        
        InetAddress addr3 = InetAddress.getByName("255.0.0.0");
        assertFalse(NetworkUtils.isValidAddress(addr3));
        assertFalse(NetworkUtils.isValidAddress(addr3.getAddress()));
        
        InetAddress addr4 = InetAddress.getByName("255.0.1.2");
        assertFalse(NetworkUtils.isValidAddress(addr4));
        assertFalse(NetworkUtils.isValidAddress(addr4.getAddress()));
        
        InetAddress addr5 = InetAddress.getByName("[2001:db8::1428:57ab]");
        assertInstanceof(Inet6Address.class, addr5);
        assertFalse(NetworkUtils.isValidAddress(addr5));
        assertFalse(NetworkUtils.isValidAddress(addr5.getAddress()));
        
        // And two valid addresses
        InetAddress addr6 = InetAddress.getByName("212.0.0.0");
        assertTrue(NetworkUtils.isValidAddress(addr6));
        assertTrue(NetworkUtils.isValidAddress(addr6.getAddress()));
        
        InetAddress addr7 = InetAddress.getByName("[2001:db9::1428:57ab]");
        assertTrue(NetworkUtils.isValidAddress(addr7));
        assertTrue(NetworkUtils.isValidAddress(addr7.getAddress()));
    }
    
    /**
     * Test to make sure the method for checking for valid ports is working.
     */
	public void testNetworkUtilsPortCheck() {
		int port = -1;
		assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
        port = 0;
        assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
		port = 99999999;
		assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
		port = 20;
		assertTrue("port should be valid", NetworkUtils.isValidPort(port));
        
        port = Short.MAX_VALUE+1;
        assertTrue("port should be valid: " + port, NetworkUtils.isValidPort(port));
        
        port = 0xFFFF + 1;
        assertFalse("port should not be valid: " + port, NetworkUtils.isValidPort(port));
	}

    /**
     * Tests the ip2string method.
     */
    public void testIP2String() throws Exception {
        byte[] buf=new byte[10];
        buf[3]=(byte)192;
        buf[4]=(byte)168;
        buf[5]=(byte)0;
        buf[6]=(byte)1;       
        assertEquals("192.168.0.1", NetworkUtils.ip2string(buf, 3));
        
        buf=new byte[4];
        buf[0]=(byte)0;
        buf[1]=(byte)1;
        buf[2]=(byte)2;
        buf[3]=(byte)3;
        assertEquals("0.1.2.3", NetworkUtils.ip2string(buf));

        buf=new byte[4];
        buf[0]=(byte)252;
        buf[1]=(byte)253;
        buf[2]=(byte)254;
        buf[3]=(byte)255;
        assertEquals("252.253.254.255",NetworkUtils.ip2string(buf));        
    }
    
    
    public void testGetAddressV6Bytes() throws UnknownHostException {
        
        for (String name : new String[] { "127.0.0.1", "255.255.255.0", 
                "192.168.0.1", "128.0.245.90" }) {
            InetAddress addr = InetAddress.getByName(name);
            byte[] bytes = NetworkUtils.getIPv6AddressBytes(addr);
            assertEquals(16, bytes.length);
            InetAddress res = InetAddress.getByAddress(bytes); 
            assertTrue(res instanceof Inet4Address);
            assertEquals(addr, res);
        }
        
        for (String name : new String[] { "[::]", "[::1]", "[2001:db8::]", 
                "[2001:0db8:0000:0000:0000:0000:1428:57ab]",
                "[2001:0db8::1428:57ab]"}) {
            InetAddress addr = InetAddress.getByName(name);
            byte[] bytes = NetworkUtils.getIPv6AddressBytes(addr);
            assertEquals(addr.getAddress(), bytes);
            InetAddress res = InetAddress.getByAddress(bytes);
            assertTrue(res instanceof Inet6Address);
            assertEquals(addr, res);
        }
    }
    
    public void testIsPrivateAddressIPv6() throws UnknownHostException {
        // A private IPv4-mapped address 
        InetAddress addr1 = InetAddress.getByName("[::ffff:192.168.1.0]");
        assertInstanceof(Inet4Address.class, addr1);
        assertTrue(NetworkUtils.isPrivateAddress(addr1));
        assertTrue(NetworkUtils.isPrivateAddress(addr1.getAddress()));
        
        // An IPv4-compatible address but it's not private.
        InetAddress addr2 = InetAddress.getByName("[::0000:192.168.1.0]");
        assertInstanceof(Inet6Address.class, addr2);
        assertTrue(((Inet6Address)addr2).isIPv4CompatibleAddress());
        assertTrue(NetworkUtils.isPrivateIPv4CompatibleAddress(addr2));
        assertFalse(NetworkUtils.isPrivateAddress(addr2));
        assertFalse(NetworkUtils.isPrivateAddress(addr2.getAddress()));
        
        // Public IPv4 mapped address
        InetAddress addr3 = InetAddress.getByName("[::ffff:216.254.98.132]");
        assertInstanceof(Inet4Address.class, addr3);
        assertFalse(NetworkUtils.isPrivateAddress(addr3));
        assertFalse(NetworkUtils.isPrivateAddress(addr3.getAddress()));
        
        // Public IPv4 compatible address
        InetAddress addr4 = InetAddress.getByName("[::0000:216.254.98.132]");
        assertInstanceof(Inet6Address.class, addr4);
        assertFalse(NetworkUtils.isPrivateAddress(addr4));
        assertFalse(NetworkUtils.isPrivateAddress(addr4.getAddress()));
        
        // Create an IPv4-mapped address
        byte[] addr5 = new byte[16];
        addr5[10] = (byte)0xFF;
        addr5[11] = (byte)0xFF;
        addr5[12] = (byte)192;
        addr5[13] = (byte)168;
        addr5[14] = (byte)1;
        addr5[15] = (byte)0;
        
        // Check if it's an IPv4-mapped address
        assertTrue(NetworkUtils.isIPv4MappedAddress(addr5));
        
        // It's not possible to construct an Inet6Address instance
        // of an IPv4-mapped address
        assertInstanceof(Inet4Address.class, InetAddress.getByAddress(addr5));
        
        // Yes, it's private
        assertTrue(NetworkUtils.isPrivateAddress(addr5));
        
        // Create a non private address and check again
        addr5[12] = (byte)212;
        addr5[13] = (byte)1;
        addr5[14] = (byte)2;
        addr5[15] = (byte)3;
        assertTrue(NetworkUtils.isIPv4MappedAddress(addr5));
        assertFalse(NetworkUtils.isPrivateAddress(addr5));
    }
    
    public void testIsIPv4ComatibleAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.168.1.0");
        assertInstanceof(Inet4Address.class, addr);
        
        // Create an IPv4 compatible IPv6 address
        byte[] compatible = new byte[16];
        System.arraycopy(addr.getAddress(), 0, compatible, 12, addr.getAddress().length);
        assertTrue(NetworkUtils.isIPv4CompatibleAddress(compatible));
        
        // Change some bytes from 0 through 11 and it shouldn't
        // be any longer an IPv4 compatible address
        compatible[10] = (byte)0xFF;
        compatible[11] = (byte)0xFF;
        assertFalse(NetworkUtils.isIPv4CompatibleAddress(compatible));
    }
    
    public void testIsIPv4MappedAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.168.1.0");
        assertInstanceof(Inet4Address.class, addr);
        
        // Create an IPv4 mapped IPv6 address. We start with
        // a byte-array that's NOT an IPv4 mapped address!
        byte[] mapped = new byte[16];
        System.arraycopy(addr.getAddress(), 0, mapped, 12, addr.getAddress().length);
        assertFalse(NetworkUtils.isIPv4MappedAddress(mapped));
        
        // Fix the address and it should be an IPv4 mapped
        // IPv6 address now!
        mapped[10] = (byte)0xFF;
        mapped[11] = (byte)0xFF;
        assertTrue(NetworkUtils.isIPv4MappedAddress(mapped));
    }
    
    public void testIsSameAddressSpace() throws UnknownHostException {
        InetAddress addr1 = InetAddress.getByName("192.168.1.1");
        InetAddress addr2 = InetAddress.getByName("192.168.1.2");
        
        // Both instances should be IP4
        assertInstanceof(Inet4Address.class, addr1);
        assertInstanceof(Inet4Address.class, addr2);
        
        // IPv4 with IPv4
        assertTrue(NetworkUtils.isSameAddressSpace(addr1, addr2));
        
        InetAddress addr3 = InetAddress.getByName("[::AAAA:192.168.1.3]");
        InetAddress addr4 = InetAddress.getByName("[::BBBB:192.168.1.4]");
        
        // Both instances should be IPv6
        assertInstanceof(Inet6Address.class, addr3);
        assertInstanceof(Inet6Address.class, addr4);
        
        // IPv6 with IPv6
        assertTrue(NetworkUtils.isSameAddressSpace(addr3, addr4));
        
        // IPv4 with IPv6
        assertFalse(NetworkUtils.isSameAddressSpace(addr1, addr3));
    }
    
    public void testIsLocalAddress() throws UnknownHostException, SocketException {
        InetAddress addr1 = InetAddress.getByName("localhost");
        assertTrue(NetworkUtils.isLocalAddress(addr1));
        
        InetAddress addr2 = InetAddress.getByName("127.0.0.1");
        assertTrue(NetworkUtils.isLocalAddress(addr2));
        
        InetAddress addr3 = InetAddress.getLocalHost();
        assertTrue(NetworkUtils.isLocalAddress(addr3));
        
        // Get all local InetAddresses for localhost
        InetAddress[] addr4 = InetAddress.getAllByName("localhost");
        assertTrue(addr4.length > 0);
        for (InetAddress addr : addr4) {
            assertTrue(NetworkUtils.isLocalAddress(addr));
        }
        
        // Go through every NetworkInterface and every InetAddress
        // They should be all local addresses!
        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        boolean checkedAtLeastOneAddress = false;
        while(nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();
            Enumeration<InetAddress> addresses = nif.getInetAddresses();
            while(addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                assertTrue(NetworkUtils.isLocalAddress(address));
                checkedAtLeastOneAddress = true;
            }
        }
        assertTrue(checkedAtLeastOneAddress);
        
        // This is not a local address of this machine
        InetAddress addr5 = InetAddress.getByName("www.google.com");
        assertFalse(NetworkUtils.isLocalAddress(addr5));
        
        // Nor is this
        InetAddress addr6 = InetAddress.getByName("192.168.88.44");
        assertFalse(NetworkUtils.isLocalAddress(addr6));
    }
    
    public void testIsAnyLocalAddress() throws UnknownHostException {
        InetAddress addr1 = InetAddress.getByName("0.0.0.0");
        assertTrue(addr1.isAnyLocalAddress());
        assertTrue(NetworkUtils.isAnyLocalAddress(addr1.getAddress()));
        
        InetAddress addr2 = InetAddress.getByName("0.0.0.1");
        assertFalse(addr2.isAnyLocalAddress());
        assertFalse(NetworkUtils.isAnyLocalAddress(addr2.getAddress()));
        
        InetAddress addr3 = InetAddress.getByName("[0000:0000:0000:0000:0000:0000:0000:0000]");
        assertInstanceof(Inet6Address.class, addr3);
        assertTrue(addr3.isAnyLocalAddress());
        assertTrue(NetworkUtils.isAnyLocalAddress(addr3.getAddress()));
        
        InetAddress addr4 = InetAddress.getByName("[0000:0000:0000:0000:0000:0000:0000:0001]");
        assertInstanceof(Inet6Address.class, addr4);
        assertFalse(addr4.isAnyLocalAddress());
        assertFalse(NetworkUtils.isAnyLocalAddress(addr4.getAddress()));
    }
    
    public void testIsLoopbackAddress() throws UnknownHostException {
        InetAddress addr1 = InetAddress.getByName("127.0.0.1");
        assertTrue(addr1.isLoopbackAddress());
        assertTrue(NetworkUtils.isLoopbackAddress(addr1.getAddress()));
        
        InetAddress addr2 = InetAddress.getByName("128.0.0.1");
        assertFalse(addr2.isLoopbackAddress());
        assertFalse(NetworkUtils.isLoopbackAddress(addr2.getAddress()));
        
        InetAddress addr3 = InetAddress.getByName("198.168.0.1");
        assertFalse(addr3.isLoopbackAddress());
        assertFalse(NetworkUtils.isLoopbackAddress(addr3.getAddress()));
        
        InetAddress addr4 = InetAddress.getByName("10.254.0.1");
        assertFalse(addr4.isLoopbackAddress());
        assertFalse(NetworkUtils.isLoopbackAddress(addr4.getAddress()));
        
        InetAddress addr5 = InetAddress.getByName("[0000:0000:0000:0000:0000:0000:0000:0001]");
        assertInstanceof(Inet6Address.class, addr5);
        assertTrue(addr5.isLoopbackAddress());
        assertTrue(NetworkUtils.isLoopbackAddress(addr5.getAddress()));
    }
    
    public void testIsIPv4LinkLocalAddress() throws UnknownHostException {
        
        // 169.254.0.0/16
        InetAddress addr1 = InetAddress.getByName("169.254.0.1");
        assertTrue(addr1.isLinkLocalAddress());
        assertTrue(NetworkUtils.isLinkLocalAddress(addr1.getAddress()));
        
        InetAddress addr2 = InetAddress.getByName("169.255.0.1");
        assertFalse(addr2.isLinkLocalAddress());
        assertFalse(NetworkUtils.isLinkLocalAddress(addr2.getAddress()));
    }
    
    public void testIsIPv6LinkLocalAddress() throws UnknownHostException {
        // This is an IPv6 Site-Local address
        InetAddress addr11 = InetAddress.getByName("[FE80:0000:0000:0000:0000:0000:0000:0001]");
        assertTrue(addr11.isLinkLocalAddress());
        assertTrue(NetworkUtils.isLinkLocalAddress(addr11.getAddress()));
        
        // And this isn't an IPv6 Site-Local address
        InetAddress addr12 = InetAddress.getByName("[FF70:0000:0000:0000:0000:0000:0000:0001]");
        assertFalse(addr12.isLinkLocalAddress());
        assertFalse(NetworkUtils.isLinkLocalAddress(addr12.getAddress()));
    }
    
    public void testIsIPv4SiteLocalAddress() throws UnknownHostException {
        
        // 10.0.0.0/8
        InetAddress addr1 = InetAddress.getByName("10.0.0.1");
        assertTrue(addr1.isSiteLocalAddress());
        assertTrue(NetworkUtils.isSiteLocalAddress(addr1.getAddress()));
        
        // 172.16.0.0/12
        InetAddress addr2 = InetAddress.getByName("172.16.1.1");
        assertTrue(addr2.isSiteLocalAddress());
        assertTrue(NetworkUtils.isSiteLocalAddress(addr2.getAddress()));
        
        InetAddress addr3 = InetAddress.getByName("172.31.1.1");
        assertTrue(addr3.isSiteLocalAddress());
        assertTrue(NetworkUtils.isSiteLocalAddress(addr3.getAddress()));
        
        InetAddress addr4 = InetAddress.getByName("172.32.1.1");
        assertFalse(addr4.isSiteLocalAddress());
        assertFalse(NetworkUtils.isSiteLocalAddress(addr4.getAddress()));
        
        // 192.168.0.0/16
        InetAddress addr5 = InetAddress.getByName("192.168.1.1");
        assertTrue(addr5.isSiteLocalAddress());
        assertTrue(NetworkUtils.isSiteLocalAddress(addr5.getAddress()));
    }
    
    public void testIsIPv6SiteLocalAddress() throws UnknownHostException {
        // This is an IPv6 Site-Local address
        InetAddress addr11 = InetAddress.getByName("[FEC0:0000:0000:0000:0000:0000:0000:0001]");
        assertTrue(addr11.isSiteLocalAddress());
        assertTrue(NetworkUtils.isSiteLocalAddress(addr11.getAddress()));
        
        // And this isn't an IPv6 Site-Local address
        InetAddress addr12 = InetAddress.getByName("[FFC0:0000:0000:0000:0000:0000:0000:0001]");
        assertFalse(addr12.isSiteLocalAddress());
        assertFalse(NetworkUtils.isSiteLocalAddress(addr12.getAddress()));
    }
    
    public void testIsUniqueLocalUnicastAddress() throws UnknownHostException {
        // FC00::/7
        InetAddress addr1 = InetAddress.getByName("[FC00:0000:0000:0000:0000:0000:0000:0001]");
        assertTrue(NetworkUtils.isUniqueLocalUnicastAddress(addr1));
        
        // This is also an Unique Local Unicast Address
        InetAddress addr2 = InetAddress.getByName("[FD00:0000:0000:0000:0000:0000:0000:0001]");
        assertTrue(NetworkUtils.isUniqueLocalUnicastAddress(addr2));
        
        // This isn't
        InetAddress addr3 = InetAddress.getByName("[FB00:0000:0000:0000:0000:0000:0000:0001]");
        assertFalse(NetworkUtils.isUniqueLocalUnicastAddress(addr3));
    }
    
    public void testIsBroadcastAddress() throws UnknownHostException {
        InetAddress addr1 = InetAddress.getByName("255.0.0.0");
        assertTrue(NetworkUtils.isBroadcastAddress(addr1));
        
        InetAddress addr2 = InetAddress.getByName("254.0.0.0");
        assertFalse(NetworkUtils.isBroadcastAddress(addr2));
        
        // IPv6 has no broadcasts addresses
        InetAddress addr3 = InetAddress.getByName("[::0000:255.0.0.0]");
        assertFalse(NetworkUtils.isBroadcastAddress(addr3));
    }
    
    public void testIsPrivateIPv4CompatibleAddress() throws UnknownHostException {
        InetAddress addr1 = InetAddress.getByName("[::0000:10.0.0.1]");
        assertInstanceof(Inet6Address.class, addr1);
        assertTrue(((Inet6Address)addr1).isIPv4CompatibleAddress());
        assertTrue(NetworkUtils.isPrivateIPv4CompatibleAddress(addr1));
        
        InetAddress addr2 = InetAddress.getByName("[::0000:212.1.2.3]");
        assertInstanceof(Inet6Address.class, addr2);
        assertTrue(((Inet6Address)addr1).isIPv4CompatibleAddress());
        assertFalse(NetworkUtils.isPrivateIPv4CompatibleAddress(addr2));
    }
    
    public void testIsDocumentationAddress() throws UnknownHostException {
        InetAddress addr1 = InetAddress.getByName("[2001:db8::1428:57ab]");
        assertInstanceof(Inet6Address.class, addr1);
        assertTrue(NetworkUtils.isDocumentationAddress(addr1));
        
        InetAddress addr2 = InetAddress.getByName("[2001:db9::1428:57ab]");
        assertInstanceof(Inet6Address.class, addr2);
        assertFalse(NetworkUtils.isDocumentationAddress(addr2));
        
        InetAddress addr3 = InetAddress.getByName("192.168.1.1");
        assertInstanceof(Inet4Address.class, addr3);
        assertFalse(NetworkUtils.isDocumentationAddress(addr3));
    }
    
    public void testUnpackIpPorts() throws Exception {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 4, 7, 0 };
        List<IpPort> ipps = NetworkUtils.unpackIps(data);
        Iterator<IpPort> it = ipps.iterator();
        
        IpPort ipp = it.next();
        assertNotTLS(ipp);
        assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:5"), ipp));
        
        ipp = it.next();
        assertNotTLS(ipp);
        assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:6"), ipp));
        
        ipp = it.next();
        assertNotTLS(ipp);
        assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:7"), ipp));
        
        assertFalse(it.hasNext());
    }
    
    public void testUnpackIpPortsWrongSize() throws Exception {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 0, 1 };
        try {
            NetworkUtils.unpackIps(data);
            fail("expected exception");
        } catch(InvalidDataException ide) {
            assertEquals("invalid size", ide.getMessage());
        }
    }
    
    public void testUnpackIpPortsWithDecorator() throws Exception {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 4, 7, 0, 1, 2, 3, 4, 8, 0 };
        final AtomicInteger index = new AtomicInteger(0);
        Decorator<IpPort, IpPort> tlsDecorator = new Decorator<IpPort, IpPort>() {
            public IpPort decorate(IpPort input) {
                try {
                    switch(index.get()) {
                    case 0: assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:5"), input)); break;
                    case 1: assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:6"), input)); break;
                    case 2: assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:7"), input)); break;
                    case 3: assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:8"), input)); break;
                    default:
                        fail("invalid idx: " + index.get());
                    }
                    
                    if(index.getAndIncrement() % 2 == 0)
                        return new ConnectableImpl(input, true);
                    else
                        return input;
                } catch(UnknownHostException uhe) {
                    throw new RuntimeException(uhe);
                }
            }
        };
        
        List<IpPort> ipps = NetworkUtils.unpackIps(data, tlsDecorator);
        assertEquals(4, index.get());
        assertEquals(4, ipps.size());
        IpPort ipp = ipps.get(0);
        assertTLS(ipp);
        assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:5"), ipp));
        
        ipp = ipps.get(1);
        assertNotTLS(ipp);
        assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:6"), ipp));
        
        ipp = ipps.get(2);
        assertTLS(ipp);
        assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:7"), ipp));
        
        ipp = ipps.get(3);
        assertNotTLS(ipp);
        assertEquals(0, IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:8"), ipp));
    }
    
    public void testUnpackIpPortDecoratorReturnsNull() throws Exception {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 0 };
        try {
            NetworkUtils.unpackIps(data, new Decorator<IpPort, IpPort>() {
                public IpPort decorate(IpPort input) {
                    return null;
                }
                
            });
            fail("expected exception");
        } catch(InvalidDataException ide) {
            assertEquals("decorator returned null", ide.getMessage());
        }
    }
    
    public void testIsAddress() {
        assertTrue(NetworkUtils.isAddress("host"));
        assertTrue(NetworkUtils.isAddress("123.456"));
        assertTrue(NetworkUtils.isAddress("host:123"));
        assertTrue(NetworkUtils.isAddress("host:1"));
        assertTrue(NetworkUtils.isAddress("host:65535"));
        assertFalse(NetworkUtils.isAddress("host:65536"));
        assertFalse(NetworkUtils.isAddress("host:0"));
        assertFalse(NetworkUtils.isAddress("host:abc"));
        assertFalse(NetworkUtils.isAddress("host::"));
        assertFalse(NetworkUtils.isAddress(""));
        assertFalse(NetworkUtils.isAddress(":123"));
    }
    
    public void testParseInvalidIpPort() throws Exception {
        try {
            NetworkUtils.parseIpPort(":4545", false);
            fail("should not have parsed successfully");
        } catch (IOException ie) { }
        try {
            NetworkUtils.parseIpPort("127.0.0.1:", false);
            fail("should not have parsed successfully");
        } catch (IOException ie) { }
    }
    
    public void testParsePortIp() throws Exception {
        // valid one
        IpPort ipPort = NetworkUtils.parsePortIp("5454:127.0.0.1");
        assertEquals(5454, ipPort.getPort());
        assertEquals("127.0.0.1", ipPort.getAddress());
        
        // invalid ones
        try {
            NetworkUtils.parsePortIp("5454:");
            fail("should not have parsed successfully, empty host part");
        } catch (IOException ie) { }
        try {
            NetworkUtils.parsePort(":127.0.0.1");
            fail("should not have parsed successfully, empty host part");
        } catch (IOException ie) { }
    }
    
    private static void assertNotTLS(IpPort ipp) {
        if(ipp instanceof Connectable)
            assertFalse(((Connectable)ipp).isTLSCapable());
    }
    
    private static void assertTLS(IpPort ipp) {
        assertInstanceof(Connectable.class, ipp);
        assertTrue(((Connectable)ipp).isTLSCapable());
    }
    
    public void testGetBytes() throws Exception {
        byte[] big = { 127, 0, 0, 1, 0, 1 };
        assertEquals(big, NetworkUtils.getBytes(new IpPortImpl("127.0.0.1:1"), ByteOrder.BIG_ENDIAN));
        
        big = new byte[] { 127, 0, 0, 1, (byte)0xFF, (byte)0xFE };
        assertEquals(big, NetworkUtils.getBytes(new IpPortImpl("127.0.0.1:65534"), ByteOrder.BIG_ENDIAN));
        
        big = new byte[] { 127, 0, 0, 1, (byte)0xFF, (byte)0xFF };
        assertEquals(big, NetworkUtils.getBytes(new IpPortImpl("127.0.0.1:65535"), ByteOrder.BIG_ENDIAN));
        
        byte[] little = { 127, 0, 0, 1, 1, 0 };
        assertEquals(little, NetworkUtils.getBytes(new IpPortImpl("127.0.0.1:1"), ByteOrder.LITTLE_ENDIAN));
        
        little = new byte[] { 127, 0, 0, 1, (byte)0xFE, (byte)0xFF };
        assertEquals(little, NetworkUtils.getBytes(new IpPortImpl("127.0.0.1:65534"), ByteOrder.LITTLE_ENDIAN));
        
        little = new byte[] { 127, 0, 0, 1, (byte)0xFF, (byte)0xFF };
        assertEquals(little, NetworkUtils.getBytes(new IpPortImpl("127.0.0.1:65535"), ByteOrder.LITTLE_ENDIAN));
    }
    
    public void testHexMask() throws Exception {
        assertEquals(0,NetworkUtils.getHexMask(0));
        assertEquals(0xFFFFFFFF, NetworkUtils.getHexMask(32));
        try {
            NetworkUtils.getHexMask(-1);
            fail(" negative mask");
        } catch (IllegalArgumentException expected){}
        try {
            NetworkUtils.getHexMask(33);
            fail(" too big mask");
        } catch (IllegalArgumentException expected){}
        assertEquals(0xFF000000, NetworkUtils.getHexMask(8));
        assertEquals(0xFFF00000, NetworkUtils.getHexMask(12));
        assertEquals(0xFFFF0000, NetworkUtils.getHexMask(16));
        assertEquals(0xFFFFF000, NetworkUtils.getHexMask(20));
        assertEquals(0xFFFFFF00, NetworkUtils.getHexMask(24));
    }
    
    public void testToByteAddress() throws Exception {
        int ineffectiveMask = 0xFFFFFFFF;
        byte[] address = new byte[] { (byte)191, 45, 33, (byte)0xFF, };
        assertEquals(address, NetworkUtils.toByteAddress(NetworkUtils.getMaskedIP(InetAddress.getByAddress(address), ineffectiveMask)));
    }
    
    public void testAreInSameSiteLocalNetwork() throws Exception {
        assertTrue(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("10.0.25.5"), InetAddress.getByName("10.0.22.0")));
        assertTrue(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("192.168.25.5"), InetAddress.getByName("192.168.2.0")));
        assertTrue(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("172.16.25.5"), InetAddress.getByName("172.16.0.0")));
        
        assertFalse(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("11.0.25.5"), InetAddress.getByName("10.0.22.0")));
        assertFalse(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("192.161.25.5"), InetAddress.getByName("192.168.2.0")));
        assertFalse(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("172.10.25.5"), InetAddress.getByName("172.16.0.0")));
        assertFalse(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("10.0.25.5"), InetAddress.getByName("192.168.22.0")));
        assertFalse(NetworkUtils.areInSameSiteLocalNetwork(InetAddress.getByName("172.16.25.5"), InetAddress.getByName("192.168.22.0")));
        
        // ipv6
        byte[] address1 = new byte[16];
        Arrays.fill(address1, (byte)5);
        byte[] address2 = new byte[16];
        Arrays.fill(address2, (byte)9);
        address1[0] = (byte)0xfe;
        address2[0] = (byte)0xfe;
        address1[1] = (byte)0xc0;
        address2[1] = (byte)0xc0;
        assertTrue(NetworkUtils.areInSameSiteLocalNetwork(address1, address2));
        address2[1] = 9;
        assertFalse(NetworkUtils.areInSameSiteLocalNetwork(address1, address2));
    }
    
    /**
     * Ensures that the respective methods in {@link NetworkUtils} match the
     * definition of {@link ConnectableImpl#INVALID_CONNECTABLE}.
     */
    public void testInvalidConnectableIsInvalid() {
        assertFalse(NetworkUtils.isValidAddress(ConnectableImpl.INVALID_CONNECTABLE.getAddress()));
        assertTrue(NetworkUtils.isValidPort(ConnectableImpl.INVALID_CONNECTABLE.getPort()));
        assertFalse(NetworkUtils.isValidIpPort(ConnectableImpl.INVALID_CONNECTABLE));
    }
    
    public void testInvalidAddressValiPort() throws Exception {
        assertFalse(NetworkUtils.isValidIpPort(new ConnectableImpl("0.0.0.0", 20144, true)));
    }
}
