package org.limewire.core.api.mojito;

import java.io.PrintWriter;

/**
 * Defines the manager interface for the Mojito DHT.
 */
public interface MojitoManager {

    /**
     * Invokes the specified command on the Mojito DHT, and forwards output
     * to the specified PrintWriter. 
     */
    public boolean handle(String command, PrintWriter out);
    
}
