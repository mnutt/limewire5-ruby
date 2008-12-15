package org.limewire.xmpp.client.impl;

import org.limewire.io.Address;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.XMPPAddress;

/**
 * A composite of {@link XMPPAddress} and {@link FirewalledAddress} to
 * allow connect back requests to be sent over xmpp. 
 */
public class XMPPFirewalledAddress implements Address {

    private final XMPPAddress xmppAddress;
    private final FirewalledAddress resolvedAddress;

    public XMPPFirewalledAddress(XMPPAddress xmppAddress, FirewalledAddress resolvedAddress) {
        this.xmppAddress = Objects.nonNull(xmppAddress, "xmppAddress");
        this.resolvedAddress = Objects.nonNull(resolvedAddress, "resolvedAddress");
    }
    
    @Override
    public String getAddressDescription() {
        return xmppAddress.getAddressDescription();
    }
    
    public XMPPAddress getXmppAddress() {
        return xmppAddress;
    }
    
    public FirewalledAddress getFirewalledAddress() {
        return resolvedAddress;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
