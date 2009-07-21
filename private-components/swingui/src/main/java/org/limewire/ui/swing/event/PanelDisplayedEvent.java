package org.limewire.ui.swing.event;

import java.awt.Component;



public class PanelDisplayedEvent extends AbstractEDTEvent {
    
    private final Component displayedPanel;
    
    /**
     * 
     * @param panel the Component displayed
     */
    public PanelDisplayedEvent(Component displayedPanel) {
        this.displayedPanel = displayedPanel;
    }

    /**
     * 
     * @return the component displayed
     */
    public Component getDisplayedPanel() {
        return displayedPanel;
    }
  
}
