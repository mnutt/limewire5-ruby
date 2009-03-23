package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Collection;

public interface ExcludedFolderCollectionManager {
    
    /**
     * @return true if the folder is in the list of excluded folders
     */
    boolean isExcluded(File folder);
    
    /**
     * @param folder folder to be added to the list of excluded folders
     */
    void exclude(File folder);
    
    /**
     * @param folder folder to be removed from the list of excluded folders
     */
    void restore(File folder);
    
    /**
     * @return a collection of excluded folders
     */
    Collection<File> getExcludedFolders();
    
    /**
     * unexcludes all currently excluded folders and excludes the folders in excludedFolders
     */
    void setExcludedFolders(Collection<File> excludedFolders);

}
