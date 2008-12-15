package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

public final class UpdateRequest extends AbstractVendorMessage implements VendorMessage.ControlMessage {
    private static final int OLD_KEY_VERSION = 1;    
    public static final int VERSION = 2;
    
    static final String COMPRESSED_UPDATE_KEY = "C";
    static final String UNCOMPRESSED_UPDATE_KEY = "U";
    
    private GGEP _ggep;
    private boolean parsed;

    UpdateRequest(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_REQ, version, payload, network);
    }
    
    public UpdateRequest() {
        super(F_LIME_VENDOR_ID, F_UPDATE_REQ, VERSION, DataUtils.EMPTY_BYTE_ARRAY);
    }
        
    @Override
    public String toString() {
        return "{UpdateRequest:"+super.toString()+"}";
    }
    
    public boolean isOldRequest() {
        return getVersion() == OLD_KEY_VERSION;
    }
    
    /**
     * @return the GGEP block carried in this request, if any.
     */
    public boolean hasGGEP() {
        if (_ggep == null && !parsed) {
            _ggep = parseGGEP();
            parsed = true;
        }
        return _ggep != null;
    }
    
    private GGEP parseGGEP() {
        byte [] payload = getPayload();
        if (payload == null  || payload.length == 0)
            return null;
        
        try {
            return new GGEP(payload, 0, null);
        } catch (BadGGEPBlockException bad) {
            return null;
        }
    }
    
    public boolean requestsCompressed() {
        if (!hasGGEP())
            return false;
        
        return _ggep.hasKey(COMPRESSED_UPDATE_KEY);
    }
}
