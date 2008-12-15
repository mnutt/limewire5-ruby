package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

public final class SimppRequestVM extends AbstractVendorMessage {
    
    private static final int OLD_KEY_VERSION = 1;
    public static final int VERSION = 2;

    SimppRequestVM(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP_REQ, version, payload, network); 
    }
    
    public SimppRequestVM() {
        super(F_LIME_VENDOR_ID, F_SIMPP_REQ, VERSION,
                                                DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    public boolean isOldRequest() {
        return getVersion() == OLD_KEY_VERSION;
    }
    
    @Override
    public String toString() {
        return "{SimppRequestVM:"+super.toString()+"}";
    }
    
}
