package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.GUID;

/**
 * Response for an inspection request.
 */
public class InspectionResponse extends AbstractVendorMessage {
    
    
    /** a bunch of ggep keys */
    public static final String DATA_KEY = "D";
    public static final String CHUNK_ID_KEY = "I";
    public static final String TOTAL_CHUNKS_KEY = "T";
    public static final String LENGTH_KEY = "L";
    /** 
     * How much data to put in each packet.  Must be less than the MTU.
     */
    public static final int PACKET_SIZE = 1300; // give some room for GGEP & headers

    public InspectionResponse(int version, byte[] guid, byte [] payload) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_RESP, version, payload);
        setGUID(new GUID(guid));
    }
    
    /**
     * @return true if this response contains anything and 
     * should be sent.
     */
    public boolean shouldBeSent() {
        return getPayload().length > 0;
    }
}
