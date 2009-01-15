package org.limewire.ui.swing.util;

/**
 * Defines a listener that is notified when the enabled state changes.
 */
public interface EnabledListener {

    /**
     * Handles a change in the enabled state to the specified value.
     */
    void enabledChanged(boolean enabled);

}
