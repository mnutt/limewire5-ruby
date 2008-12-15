package org.limewire.xmpp.api.client;

import org.limewire.io.PermanentAddress;
import org.limewire.util.Objects;

/**
 * Provides a permanent address for a full jabber id including
 * its resource. 
 */
public class XMPPAddress implements PermanentAddress {

    private final String id;

    /**
     * 
     * @param id the full jabber id including resource
     */
    public XMPPAddress(String id) {
        this.id = Objects.nonNull(id, "id");
    }
    
    /**
     * Returns the full jabber id including resource. 
     */
    public String getFullId() {
        return id;
    }
    
    @Override
    public String getAddressDescription() {
        return id;
    }
    
    /**
     * Returns the jabber id email address without resource. 
     */
    public String getId() {
        return parseBareAddress(id);
    }

    @Override
    public String toString() {
        return org.limewire.util.StringUtils.toString(this);
    }
    
    /**
     * Returns the XMPP address with any resource information removed. For example,
     * for the address "matt@jivesoftware.com/Smack", "matt@jivesoftware.com" would
     * be returned.
     *
     * @param XMPPAddress the XMPP address.
     * @return the bare XMPP address without resource information.
     */
    public static String parseBareAddress(String XMPPAddress) {
        if (XMPPAddress == null) {
            return null;
        }
        int slashIndex = XMPPAddress.indexOf("/");
        if (slashIndex < 0) {
            return XMPPAddress;
        }
        else if (slashIndex == 0) {
            return "";
        }
        else {
            return XMPPAddress.substring(0, slashIndex);
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof XMPPAddress)) {
            return false;
        }
        XMPPAddress other = (XMPPAddress)obj;
        return id.equals(other.id);
    }
}
