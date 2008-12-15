package org.limewire.ui.swing.search;
/**
 * Callback interface for notification of events that would clear row selection. 
 * This listener is called before and after such an event to give it a chance
 * to do whatever is appropriate to preserve row selection. 
 */
public interface RowPresevationListener {
    void preserveRowSelection();

    void restoreRowSelection();
}
