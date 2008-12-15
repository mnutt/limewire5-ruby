package org.limewire.io;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class NetworkInstanceUtilsTest extends BaseTestCase {
    
    private NetworkInstanceUtils networkInstanceUtils;
    private LocalSocketAddressProviderStub stubProvider;
    
    public NetworkInstanceUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NetworkInstanceUtilsTest.class);
    }
    
    @Override
    public void setUp() {
        stubProvider = new LocalSocketAddressProviderStub();
        networkInstanceUtils = new NetworkInstanceUtilsImpl(stubProvider, null);
    }    

    /**
     * Tests the isMe method.
     */
    public void testIsMe() throws Exception {
        stubProvider.setLocalPort(6346);
        assertTrue(networkInstanceUtils.isMe("localhost", 6346));
        assertTrue(networkInstanceUtils.isMe("127.1.2.1", 6346));            
        assertTrue(networkInstanceUtils.isMe(new byte[] { (byte)127, 0, 0, 0 }, 6346));
        
        stubProvider.setLocalPort(6345);
        assertFalse(networkInstanceUtils.isMe("localhost", 6346));
        assertFalse(networkInstanceUtils.isMe("127.1.2.1", 6346));            
        assertFalse(networkInstanceUtils.isMe(new byte[] { (byte)127, 0, 0, 0 }, 6346));
        
        stubProvider.setLocalPort(6346);
        stubProvider.setLocalAddress(new byte[] {(byte)123, (byte)132, (byte)231, 0});
        assertTrue(networkInstanceUtils.isMe("123.132.231.0", 6346));        
        assertTrue(networkInstanceUtils.isMe(new byte[] {(byte)123, (byte)132, (byte)231, 0}, 6346));

        assertFalse(networkInstanceUtils.isMe("123.132.231.1", 6346));        
        assertFalse(networkInstanceUtils.isMe(new byte[] {(byte)123, (byte)132, (byte)231, 1}, 6346));
        
    }

}
