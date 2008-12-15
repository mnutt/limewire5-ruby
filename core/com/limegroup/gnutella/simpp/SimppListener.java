package com.limegroup.gnutella.simpp;

/**
 * Listener for updates to Simpp.
 */
public interface SimppListener {
    /**
     * Notification that simpp has updated.
     * @param newVersion the version of the new simpp message
     */
    public void simppUpdated(int newVersion);
}
