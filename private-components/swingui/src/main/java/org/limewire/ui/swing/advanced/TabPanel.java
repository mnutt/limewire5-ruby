package org.limewire.ui.swing.advanced;

import javax.swing.JPanel;

import org.limewire.ui.swing.util.EnabledListener;
import org.limewire.ui.swing.util.EnabledListenerList;

/**
 * A base panel for tab content in the Advanced Tools window.
 */
public abstract class TabPanel extends JPanel {
    
    /** List of listeners notified when the tabEnabled state changes. */
    private final EnabledListenerList enabledListenerList = new EnabledListenerList();
    
    /**
     * Constructs a TabPanel.
     */
    public TabPanel() {
        super();
    }
    
    /**
     * Adds the specified listener to the list that is notified when the 
     * tabEnabled state changes.
     */
    public void addEnabledListener(EnabledListener listener) {
        enabledListenerList.addEnabledListener(listener);
    }
    
    /**
     * Removes the specified listener from the list that is notified when the 
     * tabEnabled state changes.
     */
    public void removeEnabledListener(EnabledListener listener) {
        enabledListenerList.removeEnabledListener(listener);
    }
    
    /**
     * Returns true if the tab content is enabled.
     */
    public abstract boolean isTabEnabled();

    /**
     * Performs startup tasks for the tab.  This method is called when the 
     * parent window is opened.
     */
    public abstract void start();
    
    /**
     * Performs clean up tasks for the tab.  This method is called when the
     * parent window is closed.
     */
    public abstract void stop();

    /**
     * Notifies all registered listeners that the enabled state has changed to
     * the specified value.
     */
    public void fireEnabledChanged(boolean enabled) {
        enabledListenerList.fireEnabledChanged(enabled);
    }
    
}
