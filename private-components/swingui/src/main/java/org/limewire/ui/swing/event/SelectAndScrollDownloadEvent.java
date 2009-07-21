package org.limewire.ui.swing.event;

import org.limewire.core.api.URN;

/**
 * Event to indicate that an item being downloaded should be selected. 
 */
public class SelectAndScrollDownloadEvent extends AbstractEDTEvent {
    private URN file;

    
    /**
     * @param file the item to be selected
     */
    public SelectAndScrollDownloadEvent(URN file){
        this.file =file;
    }
    
    /**
     * 
     * @return the item to be selected
     */
    public URN getSelectedURN(){
        return file;
    }

}
