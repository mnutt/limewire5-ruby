package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.Category;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public class ManagedFileListStub extends AbstractFileListStub implements ManagedFileList {

    @Override
    public Collection<File> getDirectoriesWithImportedFiles() {
        return Collections.emptySet();
    }
    
    @Override
    public void removeFolder(File folder) {
    }
    
    @Override
    public Collection<Category> getManagedCategories() {
        return EnumSet.allOf(Category.class);
    }
    
    @Override
    public List<File> getDirectoriesToExcludeFromManaging() {
        return new ArrayList<File>();
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
    }

    @Override
    public ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        throw new UnsupportedOperationException();   
    }

    @Override
    public ListeningFuture<FileDesc> fileRenamed(File oldName, File newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getDefaultManagedExtensions() {
        return Collections.emptySet();
    }

    @Override
    public List<File> getDirectoriesToManageRecursively() {
        return Collections.emptyList();
    }

    @Override
    public Map<Category, Collection<String>> getExtensionsPerCategory() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isLoadFinished() {
        return true;
    }

    @Override
    public void removeManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedExtensions(Collection<String> extensions) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return folder.isDirectory();
    }
    
    @Override
    public boolean isDirectoryExcluded(File folder) {
        return false;
    }

    @Override
    public boolean isProgramManagingAllowed() {
        return false;
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedOptions(
            Collection<File> recursiveFoldersToManage, Collection<File> foldersToExclude,
            Collection<Category> managedCategories) {
        throw new UnsupportedOperationException();
    }
}
