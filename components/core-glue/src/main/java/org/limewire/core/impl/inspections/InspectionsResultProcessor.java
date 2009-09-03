package org.limewire.core.impl.inspections;

/**
 * Used to specify what to do with inspection results
 */
public interface InspectionsResultProcessor {

    /**
     * Process inspection results
     * 
     * @param insps inspection results
     * @throws InspectionProcessingException upon error
     */
    public void inspectionsPerformed(InspectionDataContainer insps) throws InspectionProcessingException;
    
    // todo: maybe put in a "stopped()" method to process inspections when inspections communicator shuts down
}
