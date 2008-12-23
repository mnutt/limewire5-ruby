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
import com.limegroup.gnutella.downloader.RemoteFileDescCreator;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Creates {@link XMPPRemoteFileDesc} for {@link XMPPAddress}.
 */
@Singleton
public class XMPPRemoteFileDescCreator implements RemoteFileDescCreator {

    private final AddressFactory addressFactory;
    private final XMPPAddressResolver addressResolver;

    @Inject
    public XMPPRemoteFileDescCreator(AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        this.addressFactory = addressFactory;
        this.addressResolver = addressResolver;
    }
    
    @Inject
    void register(RemoteFileDescFactory remoteFileDescFactory) {
        remoteFileDescFactory.register(this);
    }
    
    @Override
    public boolean canCreateFor(Address address) {
        return address instanceof XMPPAddress;
    }

    @Override
    public RemoteFileDesc create(Address address, long index, String filename, long size,
            byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            String vendor, long createTime, boolean http1) {
        return new XMPPRemoteFileDesc((XMPPAddress)address, index, filename, size, clientGUID, speed, chat, quality, browseHost, xmlDoc, urns, replyToMulticast, vendor, createTime, addressFactory, addressResolver);
    }

}
