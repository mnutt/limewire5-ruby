package org.limewire.listener;

/**
 * @see org.limewire.listener.CachingEventMulticasterImpl
 */
public enum BroadcastPolicy {
    /**
     * broadcast every event
     */
    ALWAYS,

    /**
     * ony broadcast events if the are not equal
     * to the previous event
     */
    IF_NOT_EQUALS
}
