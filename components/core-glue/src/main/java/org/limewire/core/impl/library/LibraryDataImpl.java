/**
 * 
 */
package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryData;

import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.library.ManagedFileList;

class LibraryDataImpl implements LibraryData {

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