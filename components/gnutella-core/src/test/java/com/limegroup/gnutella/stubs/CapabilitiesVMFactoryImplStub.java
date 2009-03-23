package com.limegroup.gnutella.stubs;

import java.util.Map;
import java.util.TreeMap;

import org.limewire.collection.Comparators;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactoryImpl;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

@Singleton
public class CapabilitiesVMFactoryImplStub extends CapabilitiesVMFactoryImpl {

    Map<byte[], Integer> added = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
    
    @Inject
    public CapabilitiesVMFactoryImplStub(Provider<DHTManager> dhtManager,
            Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
            Provider<NetworkManager> networkManager) {
        super(dhtManager, simppManager, updateHandler, networkManager);
    }

    public void addMessageBlock(byte[] name, int version) {
        added.put(name, version);
        updateCapabilities();
    }
    
    @Override
    protected Map<byte[], Integer> getSupportedMessages() {
        Map<byte[], Integer> methods = super.getSupportedMessages();
        methods.putAll(added);
        return methods;
    }
    
}
