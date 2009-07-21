package org.limewire.ui.swing.event;

/**
 * Event to indicate that the Options/Preferences dialog has been
 * requested by a user action. 
 */
public class OptionsDisplayEvent extends AbstractEDTEvent {
    private String selectedPanel;

    /**
     * No panel specified.
     */
    public OptionsDisplayEvent(){}
    
    /**
     * @param selectedPanel the panel in the options dialog to be shown.  null will show the default panel.
     */
    public OptionsDisplayEvent(String selectedPanel){
        this.selectedPanel =selectedPanel;
    }
    
    /**
     * 
     * @return the name of the panel in the options dialog to be displayed. null if no panel is specified.
     */
    public String getSelectedPanel(){
        return selectedPanel;
    }

}
