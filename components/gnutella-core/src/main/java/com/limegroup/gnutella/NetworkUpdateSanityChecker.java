package com.limegroup.gnutella;

/**
 * A sanity checker for many different in-network verification
 * requests.
 * 
 * If we cannot verify the message as received from a number
 * of hosts from different areas, message the user to hit
 * the website as the installation may possible be corrupted.
 */
public interface NetworkUpdateSanityChecker {

    public static enum RequestType {
        SIMPP, VERSION;
    }

    /**
     * Stores knowledge that we've requested a network-updatable component
     * from the given source.
     */
    public void handleNewRequest(ReplyHandler handler, RequestType type);

    /**
     * Acknowledge we received a valid response from the source.
     */
    public void handleValidResponse(ReplyHandler handler, RequestType type);

    /**
     * Acknowledge we received an invalid response from the source.
     */
    public void handleInvalidResponse(ReplyHandler handler, RequestType type);
}
