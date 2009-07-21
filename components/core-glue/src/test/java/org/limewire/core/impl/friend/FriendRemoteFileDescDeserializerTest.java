package org.limewire.core.impl.friend;

import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.net.address.AddressFactory;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class FriendRemoteFileDescDeserializerTest extends BaseTestCase {
    
    public FriendRemoteFileDescDeserializerTest(String name) {
        super(name);
    }
    
    public void testRegister() {
        Mockery context = new Mockery();

        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final FriendRemoteFileDescDeserializer deserialiser
            = new FriendRemoteFileDescDeserializer(null, null);
        
        context.checking(new Expectations() {
            {   exactly(1).of(remoteFileDescFactory).register(with(any(String.class)), with(same(deserialiser)));
            }});
        
        deserialiser.register(remoteFileDescFactory);
        
        context.assertIsSatisfied();
        
    }
    
    /**
     * Confirm FriendAddressResolver is properly stored and functional 
     * after a clone.
     */
    public void testCreateWithAddressResolver() {

        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final FriendAddressResolver addressResolver = context.mock(FriendAddressResolver.class);
        
        final FriendRemoteFileDescDeserializer deserialiser
            = new FriendRemoteFileDescDeserializer(addressFactory, addressResolver);
                        
        final FriendAddress address = context.mock(FriendAddress.class);
        
        context.checking(new Expectations() {
            {  // Ensure address resolver is active
               exactly(1).of(addressResolver).getPresence(address);
               will(returnValue(null));
            }});
        
        final long index = -10;
        final String fileName = "Aaaaaaaaasdasfffffffffffffffffffffffffffoiwequrfweiorwe";
        final long size = Constants.MAX_FILE_SIZE;
        final byte[] clientGUID = new byte[] {};
        final int speed = Integer.MAX_VALUE;
        final int quality = Integer.MIN_VALUE;
        final LimeXMLDocument xmlDoc = null;
        final Set<URN> urns = new HashSet<URN>();
        final String vendor = null;
        final long createTime = Long.MIN_VALUE;
        final boolean http11 = true;
        
        RemoteFileDesc rfdNew = deserialiser.createRemoteFileDesc(address, index, fileName, size, clientGUID,
                speed, quality, xmlDoc, urns, vendor, createTime);
        
        assertEquals(address, rfdNew.getAddress());
        assertEquals(index, rfdNew.getIndex());
        assertEquals(fileName, rfdNew.getFileName());
        assertEquals(size, rfdNew.getSize());
        assertEquals(clientGUID, rfdNew.getClientGUID());
        assertEquals(speed, rfdNew.getSpeed());
        assertEquals(quality, rfdNew.getQuality());;
        assertTrue(rfdNew.isBrowseHostEnabled());
        assertEquals(xmlDoc, rfdNew.getXMLDocument());
        assertFalse(rfdNew.isReplyToMulticast());
        assertEquals(urns, rfdNew.getUrns());
        assertEquals(vendor, rfdNew.getVendor());
        assertEquals(createTime, rfdNew.getCreationTime());
        assertEquals(http11, rfdNew.isHTTP11());
        assertFalse(rfdNew.isFromAlternateLocation());
        assertFalse(rfdNew.isAltLocCapable());
        assertFalse(rfdNew.isSpam());
        assertEquals(0.0f, rfdNew.getSpamRating());
        assertEquals(Status.INSECURE, rfdNew.getSecureStatus());
        assertNotNull(rfdNew.toString());
        assertNotEquals(rfdNew.toString(), "");
        
        // Invoke a method that uses addressResolver
        rfdNew.getCredentials();
        
        context.assertIsSatisfied();
    }
    
}
