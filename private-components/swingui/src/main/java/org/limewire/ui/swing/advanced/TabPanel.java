package org.limewire.ui.swing.advanced;

import javax.swing.JPanel;

/**
 * A base panel for tab content in the Advanced Tools window.
 */
public abstract class TabPanel extends JPanel {
    
    /**
     * Constructs a TabPanel.
     */
    public TabPanel() {
        super();
    }

    /**
     * Performs startup tasks for the tab content. 
     */
    public abstract void start();
    
    /**
     * Performs clean up tasks for the tab.
     */
    public abstract void stop();

}
