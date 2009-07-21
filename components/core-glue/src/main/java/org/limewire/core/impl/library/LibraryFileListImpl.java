/**
 * 
 */
package org.limewire.core.impl.library;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.BasicEventList;

import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.Library;

class LibraryFileListImpl extends LocalFileListImpl implements LibraryFileList {
    private final Library managedList;
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
    private volatile LibraryState libraryState = LibraryState.LOADING;
    
    LibraryFileListImpl(Library managedList, CoreLocalFileItemFactory coreLocalFileItemFactory) {
        super(new BasicEventList<LocalFileItem>(), coreLocalFileItemFactory);
        this.managedList = managedList;
        this.managedList.addListener(newEventListener());
        this.managedList.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                LibraryState oldState = libraryState;
                if(evt.getPropertyName().equals("hasPending")) {
                    if(Boolean.TRUE.equals(evt.getNewValue())) {
                        libraryState = LibraryState.LOADING;
                    } else {
                        libraryState = LibraryState.LOADED;
                    }
                }
                changeSupport.firePropertyChange("state", oldState, libraryState);
            }
        });
    }
    
    @Override
    protected FileCollection getCoreCollection() {
        return managedList;
    }
    
    @Override
    protected boolean containsCoreUrn(com.limegroup.gnutella.URN urn) {
        List<FileDesc> fds = managedList.getFileDescsMatching(urn);
        for(FileDesc fd : fds) {
            if(!(fd instanceof IncompleteFileDesc)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public LibraryState getState() {
        return libraryState;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void addFileProcessingListener(EventListener<FileProcessingEvent> listener) {
       managedList.addFileProcessingListener(listener);
    }

    @Override
    public void removeFileProcessingListener(EventListener<FileProcessingEvent> listener) {
        managedList.removeFileProcessingListener(listener);
    }
    
    @Override
    public void cancelPendingTasks() {
        managedList.cancelPendingTasks();
    }
}