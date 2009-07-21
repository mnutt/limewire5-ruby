package org.limewire.ui.swing.event;

/**
 * Event to indicate whether or not the DownloadPanel should be shown.
 */
public class DownloadVisibilityEvent {
    private boolean visibility;

    
    /**
     * @param visibility the visibility of the download panel
     */
    public DownloadVisibilityEvent(boolean visibility){
        this.visibility = visibility;
    }
    
    /**
     * 
     * @return whether or not the download panel should be visible
     */
    public boolean getVisibility(){
        return visibility;
    }

}
