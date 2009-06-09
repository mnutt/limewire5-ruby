package org.limewire.core.impl.xmpp;

import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.io.Address;
import org.limewire.io.InvalidDataException;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Tests the implementation of RemoteFileItemFactory under various conditions. 
 */
public class RemoteFileItemFactoryImplTest extends BaseTestCase {

    public RemoteFileItemFactoryImplTest(String name) {
        super(name);
    }
   
    /**
     * Try to create a RemoteFileItem with a presence that does not yet have an AddressFeature.
     */
    @SuppressWarnings("unchecked")
    public void testCreateWithoutAddressFeature() throws SaveLocationException, InvalidDataException {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final XMPPAddressResolver addressResolver = context.mock(XMPPAddressResolver.class);
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final long creationTime = 199400120012L;
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final FileMetaData fileMetaData = context.mock(FileMetaData.class);
        final RemoteFileDesc initialRFD = context.mock(RemoteFileDesc.class);
        
        final RemoteFileItemFactoryImpl factory
            = new RemoteFileItemFactoryImpl(
                    new XMPPRemoteFileDescDeserializer(addressFactory, addressResolver),
                    remoteFileDescFactory);
        
        context.checking(new Expectations() {{
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(false));
            
            allowing(presence).getPresenceId();
            will(returnValue("this is identification"));
            allowing(presence);
            
            Set<String> urnSet = new HashSet<String>();
            urnSet.add("urn:sha1:GLSSGFSDF43443DFSFDFSDSDUGYQYPFB");
            urnSet.add("urn:sha1:GLSSGFSSDFSDF3DFSFDFSDSDUGYQYPFB");
            
            allowing(fileMetaData).getUrns();
            will(returnValue(urnSet));
            allowing(fileMetaData);
            
            allowing(remoteFileDescFactory).createRemoteFileDesc(
                    with(any(Address.class)), with(any(long.class)), with(any(String.class)), 
                    with(any(long.class)), with(any(byte[].class)), with(any(int.class)), 
                    with(any(int.class)), with(any(boolean.class)), with(any(LimeXMLDocument.class)), 
                    with(any(Set.class)), with(any(boolean.class)), with(any(String.class)), 
                    with(any(long.class)));
            will(returnValue(initialRFD));
            
            allowing(initialRFD).getCreationTime();
            will(returnValue(creationTime));
            allowing(initialRFD);
            
        }});
        
        RemoteFileItem item = factory.create(presence, fileMetaData);
        assertNotNull(item);
        
        // Ensure the the creation time persisted into the RemoteFileItem.
        assertEquals(creationTime, item.getCreationTime());
        
        // Ensure the correct presence was swapped into the RemoteFileItem
        assertEquals(presence, item.getSources().get(0).getFriendPresence());
      
        context.assertIsSatisfied();
    }
    
    /**
     * Try to create a normal RemoteFileItem
     */
    @SuppressWarnings("unchecked")
    public void testCreateWithAddressFeature() throws SaveLocationException, InvalidDataException {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final XMPPAddressResolver addressResolver = context.mock(XMPPAddressResolver.class);
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final long creationTime = 199400120012L;
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final FileMetaData fileMetaData = context.mock(FileMetaData.class);
        final RemoteFileDesc initialRFD = context.mock(RemoteFileDesc.class);
        final XMPPAddress swapAddress = context.mock(XMPPAddress.class);
        
        final RemoteFileItemFactoryImpl factory
            = new RemoteFileItemFactoryImpl(
                    new XMPPRemoteFileDescDeserializer(addressFactory, addressResolver),
                    remoteFileDescFactory);
        
        context.checking(new Expectations() {{
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(true));
            
            AddressFeature feature = context.mock(AddressFeature.class);
            allowing(presence).getFeature(AddressFeature.ID);
            will(returnValue(feature));
            allowing(feature).getFeature();
            will(returnValue(swapAddress));            
            
            allowing(presence).getPresenceId();
            will(returnValue("this is identification"));
            allowing(presence);
            
            Set<String> urnSet = new HashSet<String>();
            allowing(fileMetaData).getUrns();
            will(returnValue(urnSet));
            allowing(fileMetaData);
            
            allowing(remoteFileDescFactory).createRemoteFileDesc(
                    with(any(Address.class)), with(any(long.class)), with(any(String.class)), 
                    with(any(long.class)), with(any(byte[].class)), with(any(int.class)), 
                    with(any(int.class)), with(any(boolean.class)), with(any(LimeXMLDocument.class)), 
                    with(any(Set.class)), with(any(boolean.class)), with(any(String.class)), 
                    with(any(long.class)));
            will(returnValue(initialRFD));
            
            allowing(initialRFD).getCreationTime();
            will(returnValue(creationTime));
            allowing(initialRFD);
            
        }});
        
        RemoteFileItem item = factory.create(presence, fileMetaData);
        assertNotNull(item);
        
        // Ensure the the creation time persisted into the RemoteFileItem.
        assertEquals(creationTime, item.getCreationTime());
        
        // Ensure, in a round-about fashion, the correct presence was swapped into the RemoteFileItem
        //  and that it has the matching address
        assertEquals(swapAddress,
                item.getSources().get(0).getFriendPresence().getFeature(AddressFeature.ID).getFeature());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Try to create a RemoteFileItem but with an invalid URN list.  Ensure the error 
     *  is caught and handled properly.
     */
    @SuppressWarnings("unchecked")
    public void testCreateWithBadURN() throws SaveLocationException {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final XMPPAddressResolver addressResolver = context.mock(XMPPAddressResolver.class);
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final long creationTime = 199400120012L;
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final FileMetaData fileMetaData = context.mock(FileMetaData.class);
        final RemoteFileDesc initialRFD = context.mock(RemoteFileDesc.class);
        
        final RemoteFileItemFactoryImpl factory
            = new RemoteFileItemFactoryImpl(
                    new XMPPRemoteFileDescDeserializer(addressFactory, addressResolver),
                    remoteFileDescFactory);
        
        context.checking(new Expectations() {{
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(false));
            
            allowing(presence).getPresenceId();
            will(returnValue("this is identification"));
            allowing(presence);
            
            Set<String> urnSet = new HashSet<String>();
            urnSet.add("this is n:o:t comprehendable");
            
            allowing(fileMetaData).getUrns();
            will(returnValue(urnSet));
            allowing(fileMetaData);
            
            allowing(remoteFileDescFactory).createRemoteFileDesc(
                    with(any(Address.class)), with(any(long.class)), with(any(String.class)), 
                    with(any(long.class)), with(any(byte[].class)), with(any(int.class)), 
                    with(any(int.class)), with(any(boolean.class)), with(any(LimeXMLDocument.class)), 
                    with(any(Set.class)), with(any(boolean.class)), with(any(String.class)), 
                    with(any(long.class)));
            will(returnValue(initialRFD));
            
            allowing(initialRFD).getCreationTime();
            will(returnValue(creationTime));
            allowing(initialRFD);
            
        }});
        
        try {
            factory.create(presence, fileMetaData);
            fail("Creation should have thrown an exception when creating the URN list");
        } catch (InvalidDataException e) {
            // Expected
        }
        
        context.assertIsSatisfied();
    }
}
