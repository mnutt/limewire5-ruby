package com.limegroup.gnutella.version;

import java.io.IOException;

/** Verifies an update message. */
public interface UpdateMessageVerifier {
    
    /** Returns the XML from verifying the byte[]. */
    public String getVerifiedData(byte[] data);
    
    /** Inflates any data from the network. */
    public byte[] inflateNetworkData(byte[] input) throws IOException;

}
