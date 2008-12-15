package com.limegroup.gnutella.auth;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;

/** 
 * Validates URNs with a content authority.
 */
public interface UrnValidator {

    /** Attempts to validate this URN. */
    public void validate(URN urn);

    /** Returns true is this URN is invalid. */
    public boolean isInvalid(URN urn);

    /** Returns true if this URN is valid or unknown. */
    public boolean isValid(URN urn);

    /** Adds a listener to any URN validation events. */
    public void addListener(EventListener<ValidationEvent> eventListener);
}
