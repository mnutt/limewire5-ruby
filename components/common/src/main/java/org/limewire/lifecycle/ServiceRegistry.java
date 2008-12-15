package org.limewire.lifecycle;

public interface ServiceRegistry {
    
    StagedRegisterBuilder register(Service service);
    
    /** Runs initialize on all registered services. */
    void initialize();
    
    /** Starts all services that are *not* registered with a custom stage. */
    void start();
    
    /** Stops all services that were started in the reverse order than they were started. */
    void stop();
    
    /** Starts all services within the given stage. */
    void start(Object stage);
    
    void addListener(ServiceRegistryListener serviceRegistryListener);
    

}
