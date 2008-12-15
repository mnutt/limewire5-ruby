package org.limewire.ui.swing.util;

public interface ForceInvisibleComponent extends VisibleComponent {
    /**
     * 
     * @param isInvisible hides component if true.  This will prevent toggleVisibility() and setVisibility() from working until it is set to false.
     */
    void forceInvisibility(boolean isInvisible);
}
