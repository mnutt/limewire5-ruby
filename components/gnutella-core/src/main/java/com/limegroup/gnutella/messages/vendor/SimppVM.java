package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;

public final class SimppVM extends AbstractVendorMessage implements VendorMessage.ControlMessage {
    
    private static final int OLD_KEY_VERSION = 1;
    private static final int NEW_KEY_VERSION = 2;
    public static final int VERSION = 2;

    /**
     * Constructs a new SimppVM message from the network.
     */
    SimppVM(byte[] guid, byte ttl, byte hops, int version, byte[] payload, Network network) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP, version, payload, network);           
    }
    
    /**
     * Constructs an outgoing Simpp Message with the payload being the signed
     * parameter body.
     */
    private SimppVM(byte[] body, int version) {
        super(F_LIME_VENDOR_ID, F_SIMPP, version, body);
    }

    public byte[] getData() {
        return super.getPayload();
    }

    public static SimppVM createSimppResponse(SimppRequestVM simppReq, byte[] data) {
        if(simppReq.isOldRequest()) {
            return new SimppVM(data, OLD_KEY_VERSION);
        } else {
            return new SimppVM(data, VERSION);
        }
    }
    
    public boolean isNewVersion() {
        return getVersion() >= NEW_KEY_VERSION;
    }
    
}
