package com.limegroup.gnutella.messages.vendor;

import java.nio.ByteOrder;

import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/**
 * Message requesting inspection for specified values.
 * 
 * Note this is very LimeWire-specific, so other vendors will
 * almost certainly have no use for supporting this message.
 */
public class InspectionRequestImpl extends RoutableGGEPMessage implements InspectionRequest {
    
    
    static final String INSPECTION_KEY = "I";
    static final String TIMESTAMP_KEY = "T";
    static final String ENCODING_KEY = "E";

    /** Send back encoded responses this often */
    private final static int DEFAULT_INTERVAL = 500;
    
    private final String[] requested;
    private final boolean timestamp, encoding;
    private final int sendInterval;
    
    public InspectionRequestImpl(byte[] guid, byte ttl, byte hops, 
            int version, byte[] payload, Network network)
            throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_INSPECTION_REQ, version, payload, network);
        String requested;
        try {
            requested = ggep.getString(INSPECTION_KEY);
            timestamp = ggep.hasKey(TIMESTAMP_KEY);
            encoding = ggep.hasKey(ENCODING_KEY);
            int interval = DEFAULT_INTERVAL;
            try {
                interval = ggep.getInt(ENCODING_KEY);
            } catch (BadGGEPPropertyException noTime) {}
            sendInterval = interval;
        } catch (BadGGEPPropertyException bad) {
            throw new BadPacketException();
        }
        
        this.requested = requested.split(";"); 
    }
    
    InspectionRequestImpl(GGEPSigner signer, String... requested) {
        this(new GUID(),signer, false, false, DEFAULT_INTERVAL, 1, null, null, requested);
    }
    /**
     * @param timestamp true if the response should contain a timestamp.
     * @param requested requested fields for inspection.  
     * See <tt>InspectionUtils</tt> for description of the format.
     */
    public InspectionRequestImpl(GUID g, GGEPSigner signer, boolean timestamp, 
            boolean encoding, int sendInterval, long version, IpPort returnAddr, IpPort destAddress, String... requested) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_REQ, VERSION, signer,
                deriveGGEP(timestamp, encoding, sendInterval, version, returnAddr, destAddress, requested));
        setGUID(g);
        this.requested = requested;
        this.timestamp = timestamp;
        this.encoding = encoding;
        this.sendInterval = sendInterval;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.InspectionRequest#getRequestedFields()
     */
    public String[] getRequestedFields() {
        return requested;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.InspectionRequest#requestsTimeStamp()
     */
    public boolean requestsTimeStamp() {
        return timestamp;
    }
    
    public boolean supportsEncoding() {
        return encoding;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.InspectionRequest#setGUID(com.limegroup.gnutella.GUID)
     */
    @Override
    public void setGUID(GUID g) {
        super.setGUID(g);
    }
    
    private static GGEP deriveGGEP(boolean timestamp, boolean encoding, int sendInterval, long version, IpPort returnAddr, IpPort destAddr, String... requested) {
        /*
         * The selected fields are catenated and put in a compressed
         * ggep entry.
         */
        StringBuilder b = new StringBuilder();
        for (String r : requested)
            b.append(r).append(";");
        String ret;
        if (b.charAt(b.length() - 1) == ';')
            ret = b.substring(0, b.length() - 1);
        else
            ret = b.toString();
        
        GGEP g = new GGEP();
        g.putCompressed(INSPECTION_KEY, ret.getBytes());
        if (timestamp)
            g.put(TIMESTAMP_KEY);

        if (encoding)
            g.put(ENCODING_KEY, sendInterval);
        
        if (returnAddr != null) {
            g.put(RETURN_ADDRESS_KEY, NetworkUtils.getBytes(returnAddr.getInetAddress(), returnAddr
                    .getPort(), ByteOrder.LITTLE_ENDIAN));
        }
        
        if (destAddr != null) {
            g.put(TO_ADDRESS_KEY, NetworkUtils.getBytes(destAddr.getInetAddress(), destAddr
                    .getPort(), ByteOrder.LITTLE_ENDIAN));
        }
        
        if (version >= 0)
            g.put(VERSION_KEY, version);
        return g;
    }
    
    @Override
    public Class<? extends Message> getHandlerClass() {
        return InspectionRequest.class;
    }
    
    public int getSendInterval() {
        return sendInterval;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Requests:\n");
        for (String request : requested) {
            builder.append(request).append("\n");
        }
        return builder.toString();
    }
}
