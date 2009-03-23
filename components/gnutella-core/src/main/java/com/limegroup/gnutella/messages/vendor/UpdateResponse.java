package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.messages.BadPacketException;

public final class UpdateResponse extends AbstractVendorMessage implements VendorMessage.ControlMessage {
    
    private static final int NON_GGEP_VERSION = 1;
    private static final int OLD_KEY_VERSION = 2;
    private static final int NEW_KEY_VERSION = 3;
    public static final int VERSION = 3;

    private byte [] update;
    
    /**
     * Constructs a new UpdateResponse message from the network.
     */
    UpdateResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload, Network network) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_RESP, version, payload, network);
        
        if (version == NON_GGEP_VERSION)
            update = payload;
        else {
            // try to parse a GGEP block
            try {
                GGEP ggep = new GGEP(payload,0,null);
                if (ggep.hasKey(UpdateRequest.UNCOMPRESSED_UPDATE_KEY))
                    update = ggep.getBytes(UpdateRequest.UNCOMPRESSED_UPDATE_KEY);
                else if (ggep.hasKey(UpdateRequest.COMPRESSED_UPDATE_KEY))
                    update = ggep.getBytes(UpdateRequest.COMPRESSED_UPDATE_KEY);
                else throw new BadPacketException("no update in GGEP?");
            } catch (BadGGEPPropertyException bad) {
                throw new BadPacketException("bad ggep property");
            } catch (BadGGEPBlockException notSoBad) {
                update = payload;
            }
        }
    }
    
    /**
     * Constructs an outgoing message with the payload being the signed parameter body.
     */
    private UpdateResponse(byte[] body, int version) {
        super(F_LIME_VENDOR_ID, F_UPDATE_RESP, version, body);
    }

    public static UpdateResponse createUpdateResponse(byte [] update, UpdateRequest request) {
        if(!request.isOldRequest()) {
            // If it's not an old request, just always toss it in a GGEP.
            byte[] ggep = createGGEP(request, update);
            return new UpdateResponse(ggep, VERSION);
        } else if (!request.hasGGEP()) {
            // If it's an old request w/o ggep, don't put in GGEP
            return new UpdateResponse(update, NON_GGEP_VERSION);
        } else {
            // Old request w/ ggep, toss in the old GGEP.
            byte[] ggep = createGGEP(request, update);
            return new UpdateResponse(ggep, OLD_KEY_VERSION);
        }
    }
    
    public boolean isNewVersion() {
        return getVersion() >= NEW_KEY_VERSION;
    }
    
    private static byte[] createGGEP(UpdateRequest request, byte[] update) {
        GGEP ggep = new GGEP();
        if (request.requestsCompressed()) {
            ggep.putCompressed(UpdateRequest.COMPRESSED_UPDATE_KEY, update);
        } else {
            ggep.put(UpdateRequest.UNCOMPRESSED_UPDATE_KEY, update);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ggep.write(baos);
        } catch (IOException bad) {
            ErrorService.error(bad);
        }
        
        return baos.toByteArray();
    }
    
    public byte[] getUpdate() {
        return update;
    }
}
