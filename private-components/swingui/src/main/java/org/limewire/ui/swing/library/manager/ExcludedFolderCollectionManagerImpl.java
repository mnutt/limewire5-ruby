package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

public class ExcludedFolderCollectionManagerImpl implements ExcludedFolderCollectionManager {
    
    private final Collection<File> excludedFolders = new HashSet<File>();    

    @Override
    public void exclude(File folder) {
        excludedFolders.add(folder);
    }

    @Override
    public Collection<File> getExcludedFolders() {
        return excludedFolders;
    }

    @Override
    public boolean isExcluded(File folder) {
        return excludedFolders.contains(folder);
    }

    @Override
    public void restore(File folder) {
        excludedFolders.remove(folder);
    }

    @Override
    public void setExcludedFolders(Collection<File> excludedFolders) {
        this.excludedFolders.clear();
        this.excludedFolders.addAll(excludedFolders);
    }

}
