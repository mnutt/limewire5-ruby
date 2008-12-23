package org.limewire.core.impl.xmpp;

import java.util.Set;

import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescDeserializer;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
public class XMPPRemoteFileDescDeserializer implements RemoteFileDescDeserializer {

    private final AddressFactory addressFactory;
    private final XMPPAddressResolver addressResolver;

    @Inject
    public XMPPRemoteFileDescDeserializer(AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        this.addressFactory = addressFactory;
        this.addressResolver = addressResolver;
    }
    
    @Override
    @Inject
    public void register(RemoteFileDescFactory remoteFileDescFactory) {
        remoteFileDescFactory.register(XMPPRemoteFileDesc.TYPE, this);
    }
    
    @Override
    public RemoteFileDesc createRemoteFileDesc(Address address, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            String vendor, long createTime) {
        return new XMPPRemoteFileDesc((XMPPAddress)address, index, filename, size, clientGUID, speed, chat, quality, browseHost, xmlDoc, urns, replyToMulticast, vendor, createTime, addressFactory, addressResolver);
    }

    /**
     * Creates a {@link XMPPRemoteFileDesc} clone of remote file desc replacing the address with <code>address</code>.
     */
    public RemoteFileDesc createClone(RemoteFileDesc rfd, XMPPAddress address) {
       return new XMPPRemoteFileDesc(address, rfd.getIndex(), rfd.getFileName(), rfd.getSize(), rfd.getClientGUID(), rfd.getSpeed(), rfd.isChatEnabled(), rfd.getQuality(), rfd.isBrowseHostEnabled(), 
               rfd.getXMLDocument(), rfd.getUrns(), rfd.isReplyToMulticast(), rfd.getVendor(), rfd.getCreationTime(), addressFactory, addressResolver); 
    }
}
