package com.limegroup.gnutella.lws.server;

import java.util.Map;

/**
 * Defines the interface to handle commands sent to a {@link LWSManager}.
 */
public interface LWSManagerCommandResponseHandler {

    /**
     * Perform some operation on the incoming message and return the result.
     * 
     * @param args CGI params
     * @return the result of performing some operation on the incoming
     *         message
     */
    String handle(Map<String, String> args);

    /**
     * Returns the unique name of this instance.
     * 
     * @return the unique name of this instance
     */
    String name();
}