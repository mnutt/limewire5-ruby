package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all files that this library is managing.
 * This list can include files that are shared, are not shared,
 * are files from the store, are shared with friends, are incomplete, etc...
 * 
 * Inclusion in this list means only that LimeWire knows about this file.
 */
public interface Library extends FileCollection {
    
    void addManagedListStatusListener(EventListener<LibraryStatusEvent> listener);
    void removeManagedListStatusListener(EventListener<LibraryStatusEvent> listener);
    
    void addPropertyChangeListener(PropertyChangeListener listener);
    void removePropertyChangeListener(PropertyChangeListener listener);
  
    /** Returns true if the initial load of the library has finished. */
    boolean isLoadFinished();
    
    /** Informs the library that the file 'oldName' has been renamed to 'newName'. */
    ListeningFuture<FileDesc> fileRenamed(File oldName, File newName);
    
    /** Informs the library that the file 'file' has changed. */
    ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs);
        
    /** Gets the set of default managed extensions.  This is not the current set. */
    Collection<String> getDefaultManagedExtensions();
    
    /** Gets the current set of managed extensions. */
    Map<Category, Collection<String>> getExtensionsPerCategory();
    
    /**
     * Sets a new collection of managed extensions that will be used
     * for when files are added from folders.
     */
    void setManagedExtensions(Collection<String> extensions);
        
    /**
     * Returns true if files in this directory are allowed to be managed.
     */
    boolean isDirectoryAllowed(File folder);
    
    /** Returns all categories that should be managed. */
    Collection<Category> getManagedCategories();
    
    /** Returns true if this is allowed to many any programs. */
    boolean isProgramManagingAllowed();
    
    /**
     * Sets what categories of files will be added when a 
     * folder is added.
     */
    void setCategoriesToIncludeWhenAddingFolders(Collection<Category> managedCategories);
    
    void addFileProcessingListener(EventListener<FileProcessingEvent> listener);
    
    void removeFileProcessingListener(EventListener<FileProcessingEvent> listener);
    
    /**
     * Cancels any pending file tasks.
     */
    void cancelPendingTasks();
}
