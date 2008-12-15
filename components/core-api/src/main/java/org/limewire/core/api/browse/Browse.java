package org.limewire.core.api.browse;

public interface Browse {
    /** Starts the browse. */
    void start(BrowseListener searchListener);
    
    /** Stops the browse. */
    void stop();
}
