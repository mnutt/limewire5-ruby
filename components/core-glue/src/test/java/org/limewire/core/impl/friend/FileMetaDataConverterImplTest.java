package org.limewire.core.impl.friend;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.friend.FileMetaDataConverterImpl;
import org.limewire.core.impl.friend.FriendRemoteFileDescDeserializer;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.io.Address;
import org.limewire.io.InvalidDataException;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.BaseTestCase;

import com.google.common.collect.Iterables;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Tests the implementation of FileMetaDataConverterImpl under various conditions. 
 */
public class FileMetaDataConverterImplTest extends BaseTestCase {

    public FileMetaDataConverterImplTest(String name) {
        super(name);
    }
   
    /**
     * Try to create a RemoteFileItem with a presence that does not yet have an AddressFeature.
     */
    @SuppressWarnings("unchecked")
    public void testCreateWithoutAddressFeature() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final FriendAddressResolver addressResolver = context.mock(FriendAddressResolver.class);
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final long creationTime = 199400120012L;
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final FileMetaData fileMetaData = context.mock(FileMetaData.class);
        final RemoteFileDesc initialRFD = context.mock(RemoteFileDesc.class);
        
        final FileMetaDataConverterImpl factory
            = new FileMetaDataConverterImpl(
                    new FriendRemoteFileDescDeserializer(addressFactory, addressResolver),
                    remoteFileDescFactory);
        
        context.checking(new Expectations() {{
            allowing(presence).hasFeatures(AddressFeature.ID);
            will(returnValue(false));
            
            allowing(presence).getPresenceId();
            will(returnValue("this is identification"));
            allowing(presence);
            
            URN sha1 = URN.createSHA1Urn("urn:sha1:GLSSGFSSDFSDF3DFSFDFSDSDUGYQYPFB");
            
            allowing(fileMetaData).getUrns();
            will(returnValue(Collections.singleton(sha1.toString())));
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
            allowing(initialRFD).getUrns();
            will(returnValue(Collections.singleton(sha1)));
            allowing(initialRFD);
            
        }});
        
        SearchResult item = factory.create(presence, fileMetaData);
        assertNotNull(item);
        
        // Ensure the the creation time persisted into the RemoteFileItem.
        assertEquals(creationTime, item.getProperty(FilePropertyKey.DATE_CREATED));
        
        // Ensure the correct presence was swapped into the RemoteFileItem
        assertEquals(presence, Iterables.get(item.getSources(), 0).getFriendPresence());
      
        context.assertIsSatisfied();
    }
    
    /**
     * Try to create a normal RemoteFileItem.
     */
    @SuppressWarnings("unchecked")
    public void testCreateWithAddressFeature() throws Exception {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final FriendAddressResolver addressResolver = context.mock(FriendAddressResolver.class);
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final long creationTime = 199400120012L;
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final FileMetaData fileMetaData = context.mock(FileMetaData.class);
        final RemoteFileDesc initialRFD = context.mock(RemoteFileDesc.class);
        final FriendAddress swapAddress = context.mock(FriendAddress.class);
        
        final FileMetaDataConverterImpl factory
            = new FileMetaDataConverterImpl(
                    new FriendRemoteFileDescDeserializer(addressFactory, addressResolver),
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
            
            URN sha1 = URN.createSHA1Urn("urn:sha1:GLSSGFSSDFSDF3DFSFDFSDSDUGYQYPFB");
            
            allowing(fileMetaData).getUrns();
            will(returnValue(Collections.singleton(sha1.toString())));
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
            allowing(initialRFD).getUrns();
            will(returnValue(Collections.singleton(sha1)));
            allowing(initialRFD);
            
        }});
        
        SearchResult item = factory.create(presence, fileMetaData);
        assertNotNull(item);
        
        // Ensure the the creation time persisted into the RemoteFileItem.
        assertEquals(creationTime, item.getProperty(FilePropertyKey.DATE_CREATED));
        
        // Ensure, in a round-about fashion, the correct presence was swapped into the RemoteFileItem
        //  and that it has the matching address
        assertEquals(swapAddress,
                Iterables.get(item.getSources(), 0).getFriendPresence().getFeature(AddressFeature.ID).getFeature());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Try to create a RemoteFileItem but with an invalid URN list.  Ensure the error 
     * is caught and handled properly.
     */
    @SuppressWarnings("unchecked")
    public void testCreateWithBadURN() throws DownloadException {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final FriendAddressResolver addressResolver = context.mock(FriendAddressResolver.class);
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final long creationTime = 199400120012L;
        
        final FriendPresence presence = context.mock(FriendPresence.class);
        final FileMetaData fileMetaData = context.mock(FileMetaData.class);
        final RemoteFileDesc initialRFD = context.mock(RemoteFileDesc.class);
        
        final FileMetaDataConverterImpl factory
            = new FileMetaDataConverterImpl(
                    new FriendRemoteFileDescDeserializer(addressFactory, addressResolver),
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
