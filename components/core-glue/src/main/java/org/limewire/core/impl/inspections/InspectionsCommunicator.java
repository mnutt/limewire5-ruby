package org.limewire.core.impl.inspections;

import java.util.List;

/**
 * main hub for coordinating "push inspections"
 */
public interface InspectionsCommunicator {

    /**
     * Setting an {@link InspectionsResultProcessor} for finished inspections
     * 
     * @param processor to handle inspections results
     */
    public void setResultProcessor(InspectionsResultProcessor processor);


    /**
     * Gets the current inspections result processor.
     * If one is currently not set, this method will set a default one
     * and return it.
     *
     * @return {@link InspectionsResultProcessor}
     */
    public InspectionsResultProcessor getResultProcessor();


    /**
     * Adds and initializes inspections to perform.  
     * 
     * This means scheduling inspections to be performed later given a 
     * list of specifications (containing which inspections 
     * to perform and when to perform them)
     * 
     * @param inspSpecs List specifying which inspections to perform
     * and when
     */
    public void initInspectionSpecs(List<InspectionsSpec> inspSpecs);
}
