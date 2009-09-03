package org.limewire.ui.swing.properties;

import javax.swing.JComponent;

/**
 * A subPanel that displays information about a given file.
 */
public interface FileInfoPanel {

    /**
     * Returns the Component for this panel.
     */
    public JComponent getComponent();
    
    /**
     * Returns true if any information within this panel has changed
     * during the session.
     */
    public boolean hasChanged();
    
    /**
     * Saves any information that may have changed state.
     */
    public void save();
    
    /**
     * Removes any listeners that may be registered when hiding the 
     * dialog.
     */
    public void unregisterListeners();
}
