package com.limegroup.gnutella;

public interface ApplicationServices {

    public byte[] getMyBTGUID();

    public byte[] getMyGUID();

    /** Sets full power mode. */
    public void setFullPower(boolean newValue);

}