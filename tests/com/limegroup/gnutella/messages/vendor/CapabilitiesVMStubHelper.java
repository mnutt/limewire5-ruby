package com.limegroup.gnutella.messages.vendor;

import java.util.Map;
import java.util.TreeMap;

import org.limewire.collection.Comparators;

public class CapabilitiesVMStubHelper {
    
    public static CapabilitiesVM makeCapibilitesWithSimpp(int simppNumber) throws Exception {
        Map<byte[], Integer> caps = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
        caps.put(new byte[] { 'S', 'I', 'P', 'M' }, simppNumber);
        return new CapabilitiesVMImpl(caps);
    }
    
    public static CapabilitiesVM makeCapabilitiesWithUpdate(int id) throws Exception {
        Map<byte[], Integer> caps = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
        caps.put(new byte[] { 'U', 'P', 'L', 'M' }, id);
        return new CapabilitiesVMImpl(caps);        
    }
}
