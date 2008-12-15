package org.limewire.net.address;

import java.io.IOException;

import org.limewire.io.Address;

public class MockAddressSerializer implements AddressSerializer{
    @Override
    public void register(AddressFactory factory) {
    }

    @Override
    public String getAddressType() {
        return "";
    }

    @Override
    public Class<? extends Address> getAddressClass() {
        return MockAddress.class;
    }

    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        return new MockAddress();
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        return new byte[0];
    }

    @Override
    public Address deserialize(String address) throws IOException {
        return new MockAddress();
    }
}
