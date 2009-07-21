package org.limewire.core.impl.friend;

import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.BaseTestCase;
import org.limewire.core.impl.friend.FriendRemoteFileDescCreator;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A series of tests for XMPPRemoteFileDescCreator.
 */
public class FriendRemoteFileDescCreatorTest extends BaseTestCase {

    public FriendRemoteFileDescCreatorTest(String name) {
        super(name);
    }
    
    /**
     * Test the register method and ensure the factory will actually be registered with this class.
     */
    public void testRegister() {
        Mockery context = new Mockery();
    
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final FriendRemoteFileDescCreator creator = new FriendRemoteFileDescCreator(null, null);
        
        context.checking(new Expectations() {{
            exactly(1).of(remoteFileDescFactory).register(creator);
        }});
        
        creator.register(remoteFileDescFactory);        

        context.assertIsSatisfied();
    }
    
    /** 
     * Passes a variety of Address types and ensures only a XMPPAddress will return true.
     */
    public void testCanCreateFor() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final Address plainAddress = context.mock(Address.class);
        final Address xmppAddress = context.mock(FriendAddress.class);
        final Address otherAddress = context.mock(FirewalledAddress.class);
        final Address xmppExtAddress = new FriendAddress("asda") {
            @Override
            public String getId() {
                return "hello";
            }
        };
        
        final FriendRemoteFileDescCreator creator = new FriendRemoteFileDescCreator(null, null);
        
        context.checking(new Expectations() {{
            // None
        }});
        
        assertFalse(creator.canCreateFor(plainAddress));
        assertFalse(creator.canCreateFor(otherAddress));
        assertTrue(creator.canCreateFor(xmppAddress));
        assertTrue(creator.canCreateFor(xmppExtAddress));
        
        context.assertIsSatisfied();
    }
    
    /**
     * Simple creation test.  Ensures all the values are picked up and the AddressResolver is correctly 
     *  passed on.  See XMPPRemoteFireDescDeserialiser for more tests if more concrete verification is 
     *  needed.
     */
    public void testCreateWithAddressResolver() {

        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final FriendAddressResolver addressResolver = context.mock(FriendAddressResolver.class);
        
        final FriendRemoteFileDescCreator creator
            = new FriendRemoteFileDescCreator(addressFactory, addressResolver);
                        
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
        final boolean browseHost = false;
        final boolean replyToMulticast = true;
        
        RemoteFileDesc rfdNew = creator.create(address, index, fileName, size, clientGUID,
                speed, quality, browseHost, xmlDoc, urns, replyToMulticast, vendor, createTime, http11);
        
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
