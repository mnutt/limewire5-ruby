package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.GUID;
import org.limewire.io.IpPort;


public interface InspectionRequest extends VendorMessage.ControlMessage {
    
    static final int VERSION = 1;

    public String[] getRequestedFields();

    public boolean requestsTimeStamp();

    public void setGUID(GUID g);

    public int getVersion();
    
    public byte [] getGUID();
    
    public IpPort getReturnAddress();
    
    public long getRoutableVersion();
    
    public boolean supportsEncoding();
    
    /**
     * @return the interval at which to send encoded responses.  Only makes
     * sense if supportsEncoding() returns true.
     */
    public int getSendInterval();

}