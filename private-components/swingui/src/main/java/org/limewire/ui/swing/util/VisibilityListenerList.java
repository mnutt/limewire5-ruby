package org.limewire.ui.swing.util;

import java.util.concurrent.CopyOnWriteArrayList;

public class VisibilityListenerList implements VisibilityListener {
    private final CopyOnWriteArrayList<VisibilityListener> listeners = new CopyOnWriteArrayList<VisibilityListener>();

    /**
     * Adds a listener to the list.
     */
    public void addVisibilityListener(VisibilityListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from this list.
     */
    public void removeVisibilityListener(VisibilityListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void visibilityChanged(boolean visible) {
        for (VisibilityListener listener : listeners) {
            listener.visibilityChanged(visible);
        }
    }

}
