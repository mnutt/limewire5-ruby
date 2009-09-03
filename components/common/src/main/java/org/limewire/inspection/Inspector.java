package org.limewire.inspection;

import java.io.File;

import com.google.inject.Injector;

/**
 * Inspector based on a human-readable key.
 */
public interface Inspector {
    /**
     * @param props a file containing mappings
     * from human-readable keys to inspection point paths.
     */
    void load(File props);
    
    /**
     * @return true if mappings were loaded successfully
     */
    boolean loaded();
    
    /**
     * @param key human readable key or actual path to inspect.
     * @param collectUsageData whether DataCategory.USAGE inspection points should be collected
     * @return the Object from the inspection
     * @throws InspectionException if something goes wrong 
     */
    Object inspect(String key, boolean collectUsageData) throws InspectionException;
    
    /** Sets the new Injector that will be used for inspections. */
    void setInjector(Injector injector);
}
