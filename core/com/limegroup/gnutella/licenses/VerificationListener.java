package com.limegroup.gnutella.licenses;

/**
 * A callback for verifying licenses.
 */
public interface VerificationListener {
    
    public void licenseVerified(License license);
    
}