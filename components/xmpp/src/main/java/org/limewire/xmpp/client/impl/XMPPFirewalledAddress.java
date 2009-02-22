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

    /**
     * Constructs an {@link XMPPFirewalledAddress}.
     * @param xmppAddress cannot be null
     * @param resolvedAddress cannot be null
     */
    public XMPPFirewalledAddress(XMPPAddress xmppAddress, FirewalledAddress resolvedAddress) {
        this.xmppAddress = Objects.nonNull(xmppAddress, "xmppAddress");
        this.resolvedAddress = Objects.nonNull(resolvedAddress, "resolvedAddress");
    }
    
    @Override
    public String getAddressDescription() {
        return xmppAddress.getAddressDescription();
    }
    
    /**
     * @return the {@link XMPPAddress} 
     */
    public XMPPAddress getXmppAddress() {
        return xmppAddress;
    }
    
    /**
     * @return the {@link FirewalledAddress} 
     */
    public FirewalledAddress getFirewalledAddress() {
        return resolvedAddress;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XMPPFirewalledAddress) {
            XMPPFirewalledAddress other = (XMPPFirewalledAddress)obj;
            return this.xmppAddress.equals(other.xmppAddress) && this.resolvedAddress.equals(other.resolvedAddress);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return xmppAddress.hashCode() + 31 * resolvedAddress.hashCode();
    }
}
