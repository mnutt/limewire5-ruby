package org.limewire.core.api;

/** Contains methods relating to the overall application. */
public interface Application {

    /**
     * Returns a URL that is unique according to the unique properties of
     * the running application.
     */
    String addClientInfoToUrl(String baseUrl);

    /** Returns true if the application is in a 'testing' version. */
    boolean isTestingVersion();

    /**
     * Starts the core services of the application.
     */
    void startCore();

    /**
     * Stops the core services of the application.
     */
    void stopCore();
    
    /**
     * Sets a command than will be executed after shutdown.
     */
    void setShutdownFlag(String flag);

    /** Returns the version of the program. */
    String getVersion();

    /** 
     * Returns true if this version of LimeWire is a 'Pro' Version. 
     */
    public boolean isProVersion();

}
