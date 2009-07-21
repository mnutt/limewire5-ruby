package org.limewire.friend.impl.address;

import java.io.IOException;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.io.Address;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Serializes and deserializes {@link FriendAddress} objects.
 */
@Singleton
public class FriendAddressSerializer implements AddressSerializer {

    static final String JID = "JID";
    
    @Override
    @Inject
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }
    
    @Override
    public boolean canSerialize(Address address) {
        return address instanceof FriendAddress;
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
                return new FriendAddress(address);
            }
        }
        throw new IOException();
    }
    
    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedAddress);
            return new FriendAddress(ggep.getString(JID));
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        FriendAddress friendAddress = (FriendAddress)address;
        GGEP ggep = new GGEP();
        ggep.put(JID, friendAddress.getFullId());
        return ggep.toByteArray();
    }

}
