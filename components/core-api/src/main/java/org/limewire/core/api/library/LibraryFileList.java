package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

import org.limewire.listener.EventListener;

/** An extension of LocalFileList that adds a retrievable state. */
public interface LibraryFileList extends LocalFileList {

    /** Returns the current state of the library. */
    LibraryState getState();
    
    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
    
    void addFileProcessingListener(EventListener<FileProcessingEvent> listener);
    
    void removeFileProcessingListener(EventListener<FileProcessingEvent> listener);

    /**
     * Cancels any pending file tasks.
     */
    void cancelPendingTasks();
}
