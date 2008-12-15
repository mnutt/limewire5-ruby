package org.limewire.ui.swing.util;

public interface VisibleComponent {
    /**
     * Sets the visibility of this component.
     */
    void setVisibility(boolean visible);

    /**
     * Toggles the visibility of this component.
     */
    void toggleVisibility();

    /**
     * Adds a listener to this items visibility.
     */
    void addVisibilityListener(VisibilityListener listener);

    /**
     * Removes a listener from this items visibility.
     */
    void removeVisibilityListener(VisibilityListener listener);

    /**
     * Returns true if this component is currently visible.
     */
    boolean isVisible();
}
