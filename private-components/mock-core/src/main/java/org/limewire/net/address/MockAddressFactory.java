package org.limewire.net.address;

import java.io.IOException;

import org.limewire.io.Address;

public class MockAddressFactory implements AddressFactory{
    @Override
    public void registerSerializer(AddressSerializer serializer) {
    }

    @Override
    public AddressSerializer getSerializer(Class<? extends Address> addressClass) throws IllegalArgumentException {
        return new MockAddressSerializer();
    }

    @Override
    public AddressSerializer getSerializer(String addressType) {
        return new MockAddressSerializer();
    }

    @Override
    public Address deserialize(String type, byte[] serializedAddress) throws IOException {
        return new MockAddress();
    }

    @Override
    public Address deserialize(String address) throws IOException {
        return new MockAddress();
    }
}
