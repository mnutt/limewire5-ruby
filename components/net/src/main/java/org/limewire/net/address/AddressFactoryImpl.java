package org.limewire.net.address;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Singleton;

@Singleton
public class AddressFactoryImpl implements AddressFactory {
    private static final Log LOG = LogFactory.getLog(AddressFactoryImpl.class);

    private ConcurrentHashMap<String, AddressSerializer> serializerTypeMap = new ConcurrentHashMap<String, AddressSerializer>();
    private ConcurrentHashMap<Class<? extends Address>, AddressSerializer> serializerClassMap = new ConcurrentHashMap<Class<? extends Address>, AddressSerializer>();

    public void registerSerializer(AddressSerializer serializer) {
        LOG.debugf("adding serializer type = {0}, class = {1}", serializer.getAddressType(), serializer.getAddressClass());
        serializerTypeMap.put(serializer.getAddressType(), serializer);
        serializerClassMap.put(serializer.getAddressClass(), serializer);
    }

    public AddressSerializer getSerializer(Class<? extends Address> addressClass) {
        Class c = addressClass;
        AddressSerializer serializer;
        do {
            Class[] interfaces = c.getInterfaces();
            serializer = getSerializer(c, interfaces);
            c = c.getSuperclass();
        } while (serializer == null && c != null);
        if(serializer != null) {
            return serializer;
        } else {
            throw new IllegalArgumentException("no serializer available for: " + addressClass);
        }
    }

    private AddressSerializer getSerializer(Class addressClass, Class[] interfaces) {
        AddressSerializer serializer = serializerClassMap.get(addressClass);
        if(serializer != null) {
            return serializer;
        } else {
            for(Class interfase : interfaces) {
                serializer = serializerClassMap.get(interfase);
                if(serializer != null) {
                    // TODO subclasses
                    return serializer;
                }
            }
        }
        return null;
    }

    public AddressSerializer getSerializer(String addressType) {
        return serializerTypeMap.get(addressType);
    }

    public Address deserialize(String type, byte[] serializedAddress) throws IOException {
        AddressSerializer serializer = serializerTypeMap.get(type);
        if(serializer != null) {
            return serializer.deserialize(serializedAddress);
        }
        throw new IOException("unknown message type: " + type);
    }

    @Override
    public Address deserialize(String address) throws IOException {
        for(AddressSerializer serializer : serializerTypeMap.values()) {
            try {
                return serializer.deserialize(address);
            } catch (IOException ioe) {
            }
        }
        throw new IOException();
    }
}
