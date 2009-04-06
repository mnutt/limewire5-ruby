package com.limegroup.gnutella.bootstrap;

/**
 * Defines an interface for managing a set of gwebcaches and retrieving hosts
 * from them.
 */
interface TcpBootstrap {

    /**
     * Clears the set of attempted gwebcaches.
     */
    public void resetData();

    /**
     * Attempts to contact a gwebcache to retrieve endpoints.
     */
    public boolean fetchHosts(Bootstrapper.Listener listener);

    /**
     * Loads the default set of gwebcaches.
     */
    public void loadDefaults();
}
