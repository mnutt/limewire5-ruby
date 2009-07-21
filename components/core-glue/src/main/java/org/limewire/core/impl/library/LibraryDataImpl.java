package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryData;

import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryUtils;

class LibraryDataImpl implements LibraryData {

    private final Library library;
    
    public LibraryDataImpl(Library fileList) {
        this.library = fileList;
    }
    
    @Override
    public boolean isFileManageable(File f) {
        return LibraryUtils.isFileManagable(f);
    }
    
    @Override
    public boolean isProgramManagingAllowed() {
        return library.isProgramManagingAllowed();
    }
    
    @Override
    public Collection<Category> getManagedCategories() {
        return library.getManagedCategories();
    }        

    @Override
    public boolean isDirectoryAllowed(File folder) {
        return library.isDirectoryAllowed(folder);
    }
    
    @Override
    public Collection<String> getDefaultExtensions() {
        return library.getDefaultManagedExtensions();
    }
    
    @Override
    public Map<Category, Collection<String>> getExtensionsPerCategory() {
        return library.getExtensionsPerCategory();
    }
    
    @Override
    public void setManagedExtensions(Collection<String> extensions) {
        library.setManagedExtensions(extensions);
    }
    
    @Override
    public void setCategoriesToIncludeWhenAddingFolders(Collection<Category> managedCategories) {
        library.setCategoriesToIncludeWhenAddingFolders(managedCategories);
    }
}