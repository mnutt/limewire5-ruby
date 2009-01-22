package org.limewire.core.impl.library;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.BasicEventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.library.ManagedFileList;
import com.limegroup.gnutella.library.FileListChangedEvent.Type;

@Singleton
class LibraryManagerImpl implements LibraryManager {
    
    private final LibraryFileListImpl libraryList;
    private final LibraryData libraryData;
    
    @Inject
    public LibraryManagerImpl(ManagedFileList managedList, CoreLocalFileItemFactory coreLocalFileItemFactory) {
        this.libraryList = new LibraryFileListImpl(managedList, coreLocalFileItemFactory);
        this.libraryData = new LibraryDataImpl(managedList);
    }
    
    @Override
    public LibraryFileList getLibraryManagedList() {
        return libraryList;
    }
    
    public LibraryData getLibraryData() {
        return libraryData;
    }
    
    private static class LibraryDataImpl implements LibraryData {

        private final ManagedFileList fileList;
        
        public LibraryDataImpl(ManagedFileList fileList) {
            this.fileList = fileList;
        }
        
        @Override
        public boolean isFileManageable(File f) {
            return LibraryUtils.isFileManagable(f);
        }
        
        @Override
        public boolean isProgramManagingAllowed() {
            return fileList.isProgramManagingAllowed();
        }
        
        @Override
        public void setManagedOptions(Collection<File> recursiveFoldersToManage,
                Collection<File> foldersToExclude, Collection<Category> managedCategories) {
            fileList.setManagedOptions(recursiveFoldersToManage, foldersToExclude, managedCategories);
        }
        
        @Override
        public void removeFolders(Collection<File> folders) {
            for(File folder : folders) {
                fileList.removeFolder(folder);
            }
        }
        
        @Override
        public Collection<Category> getManagedCategories() {
            return fileList.getManagedCategories();
        }
        
        @Override
        public List<File> getDirectoriesToExcludeFromManaging() {
            return fileList.getDirectoriesToExcludeFromManaging();
        }        

        @Override
        public boolean isDirectoryAllowed(File folder) {
            return fileList.isDirectoryAllowed(folder);
        }

        @Override
        public boolean isDirectoryExcluded(File folder) {
            return fileList.isDirectoryExcluded(folder);
        }
        
        @Override
        public Collection<File> getDirectoriesWithImportedFiles() {
            return fileList.getDirectoriesWithImportedFiles();
        }

        @Override
        public List<File> getDirectoriesToManageRecursively() {
            return fileList.getDirectoriesToManageRecursively();
        }
        
        @Override
        public Collection<String> getDefaultExtensions() {
            return fileList.getDefaultManagedExtensions();
        }
        
        @Override
        public Map<Category, Collection<String>> getExtensionsPerCategory() {
            return fileList.getExtensionsPerCategory();
        }
        
        @Override
        public void setManagedExtensions(Collection<String> extensions) {
            fileList.setManagedExtensions(extensions);
        }

        @Override
        public void reload() {
            Map<Category, Collection<String>> managedExtensionsPerCategory = getExtensionsPerCategory();
            Collection<String> extensions = CollectionUtils.flatten(managedExtensionsPerCategory.values());
            fileList.setManagedExtensions(extensions);
        }
        
    }

    private static class LibraryFileListImpl extends LocalFileListImpl implements LibraryFileList {
        private final ManagedFileList managedList;
        private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
        private volatile LibraryState libraryState = LibraryState.LOADING;
        
        LibraryFileListImpl(ManagedFileList managedList, CoreLocalFileItemFactory coreLocalFileItemFactory) {
            super(new BasicEventList<LocalFileItem>(), coreLocalFileItemFactory);
            this.managedList = managedList;
            this.managedList.addFileListListener(newEventListener());
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
        protected ManagedFileList getCoreFileList() {
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
        protected void collectionUpdate(Type type, boolean shared) {
        }
    }
}
