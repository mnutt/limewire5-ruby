package org.limewire.xmpp.client.impl;

import java.io.IOException;

import org.limewire.io.Address;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.xmpp.api.client.XMPPAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Serializes and deserializes {@link org.limewire.xmpp.api.client.XMPPAddress} objects.
 */
@Singleton
public class XMPPAddressSerializer implements AddressSerializer {

    static final String JID = "JID";
    
    @Override
    @Inject
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }
    
    @Override
    public Class<? extends Address> getAddressClass() {
        return XMPPAddress.class;
    }

    @Override
    public String getAddressType() {
        return "xmpp-address";
    }

    public Address deserialize(String address) throws IOException {
        // TODO replace with a real email address parser
        int atIndex = address.indexOf('@');
        if(atIndex != -1 && atIndex != address.length() - 1) {
            String host = address.substring(atIndex + 1);
            int dotIndex = host.indexOf('.');
            if(dotIndex != -1 && dotIndex != 0 && dotIndex != host.length() - 1) {
                return new XMPPAddress(address);
            }
        }
        throw new IOException();
    }
    
    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedAddress);
            return new XMPPAddress(ggep.getString(JID));
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        XMPPAddress xmppAddress = (XMPPAddress)address;
        GGEP ggep = new GGEP();
        ggep.put(JID, xmppAddress.getFullId());
        return ggep.toByteArray();
    }

}
