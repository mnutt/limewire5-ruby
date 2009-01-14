package org.limewire.ui.swing.util;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Defines a list of listeners that are notified when the enabled state changes.
 */
public class EnabledListenerList {
    private final CopyOnWriteArrayList<EnabledListener> listeners = 
        new CopyOnWriteArrayList<EnabledListener>();

    /**
     * Adds the specified listener to the list that is notified when the 
     * enabled state changes.
     */
    public void addEnabledListener(EnabledListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the specified listener from the list that is notified when the 
     * enabled state changes.
     */
    public void removeEnabledListener(EnabledListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies all registered listeners that the enabled state has changed to
     * the specified value.
     */
    public void fireEnabledChanged(boolean enabled) {
        for (EnabledListener listener : listeners) {
            listener.enabledChanged(enabled);
        }
    }
    
}
