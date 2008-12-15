package org.limewire.ui.swing.search;

/**
 * For notification before and after an event which would cause row
 * selection information to be lost.  All RowPreservationListener objects
 * passed to this type are forwarded before and after update event notices.
 */
public interface RowSelectionPreserver {
    void addRowPreservationListener(RowPresevationListener listener);

    void beforeUpdateEvent();

    void afterUpdateEvent();

}
