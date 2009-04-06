package org.limewire.ui.swing.util;

/**
 * enum to represent visibility of ui components
 */
public enum VisibilityType {
    VISIBLE,
    NOT_VISIBLE;

    public static VisibilityType valueOf(boolean isEnabled) {
        return isEnabled ? VISIBLE : NOT_VISIBLE;
    }
}