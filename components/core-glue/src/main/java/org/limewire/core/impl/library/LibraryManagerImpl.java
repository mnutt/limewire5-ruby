package org.limewire.core.impl.library;


import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.ManagedFileList;

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
}
