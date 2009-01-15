package org.limewire.core.impl.xmpp;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.core.impl.library.CoreRemoteFileItem;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.XMPPAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.util.DataUtils;

@Singleton
public class RemoteFileItemFactoryImpl implements RemoteFileItemFactory {
    private final XMPPRemoteFileDescDeserializer remoteFileDescDeserializer;
    private final RemoteFileDescFactory remoteFileDescFactory;

    @Inject
    public RemoteFileItemFactoryImpl(XMPPRemoteFileDescDeserializer remoteFileDescDeserializer, RemoteFileDescFactory remoteFileDescFactory) {
        this.remoteFileDescDeserializer = remoteFileDescDeserializer;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    public RemoteFileItem create(FriendPresence presence, FileMetaData fileMetaData) throws InvalidDataException, SaveLocationException {
        XMPPAddress presenceAddress = getAddressFromPresence(presence);

        RemoteFileDesc remoteFileDesc = createRfdFromChatResult(presenceAddress, fileMetaData);
        RemoteFileDescAdapter remoteFileDescAdapter = new RemoteFileDescAdapter(remoteFileDescDeserializer.createClone(remoteFileDesc,
                presenceAddress), IpPort.EMPTY_SET, presence);
        return new CoreRemoteFileItem(remoteFileDescAdapter);
    }
    
    private RemoteFileDesc createRfdFromChatResult(XMPPAddress address, FileMetaData fileMeta)
            throws SaveLocationException, InvalidDataException {
        byte[] clientGuid = DataUtils.EMPTY_GUID;
        
        Set<String> urnsAsString = fileMeta.getURNsAsString();
        Set<URN> urns = new HashSet<URN>();
        for (String urnStr : urnsAsString) {
            try {
                urns.add(URN.createUrnFromString(urnStr));
            } catch(IOException iox) {
                throw new InvalidDataException(iox);
            }
        }

        return remoteFileDescFactory.createRemoteFileDesc(address,
                fileMeta.getIndex(), fileMeta.getName(), fileMeta.getSize(), clientGuid,
                0, false, 0, true, null, urns, false,
                null, fileMeta.getCreateTime().getTime());
    }


    /**
     * Get the address if the presence has an address feature at the present moment.
     * Else construct an XMPPAddress with the friendPresence id.
     */
    private XMPPAddress getAddressFromPresence(FriendPresence presence) {
        if (presence.hasFeatures(AddressFeature.ID)) {
            return (XMPPAddress)((AddressFeature)presence.getFeature(AddressFeature.ID)).getFeature();
        }
        return new XMPPAddress(presence.getPresenceId());
    }
    
}
