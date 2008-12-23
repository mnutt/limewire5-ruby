package org.limewire.ui.swing.wizard;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.SystemUtils.SpecialLocations;

/**
 * Helper class that provides methods for determining the correct
 *  autoconfig values for certain limewire settings related
 *  to the directory manager.
 */
public class AutoDirectoryManageConfig {
    
   /**
    * Helper method to add a file to a set from a file string if it is not null or empty
    */
    private static void addIfSupported(String path, Set<File> directories) {
        if (path != null && !path.isEmpty()) {
            directories.add(new File(path));
        }
    }
    
    /**
     * Determines the OS specific list of directories to manage by default.
     *  Note, this list is not for certain as no checks are performed so  any results
     *  should be considered as best guesses.  
     *  
     */
    public static Set<File> getManagedDirectories() {
        Set<File> dirs = new HashSet<File>();

        if (OSUtils.isWindows() && !OSUtils.isWindowsVista()) {
            addIfSupported(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS), dirs);
            addIfSupported(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP), dirs);
        } else {
        
            addIfSupported(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS), dirs);
            addIfSupported(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP), dirs);
            
            String homePath = SystemUtils.getSpecialPath(SpecialLocations.HOME);
            
            if (homePath != null && !homePath.isEmpty()) {
                dirs.add(new File(homePath, "Downloads"));
                dirs.add(new File(homePath, "Movies"));
                dirs.add(new File(homePath, "Music"));
                dirs.add(new File(homePath, "Pictures"));
                dirs.add(new File(homePath, "Public"));
            }
        }
                
        return dirs;
    }
    
    /**
     * Determines the OS specific list of directories to exclude
     */
    public static  Set<File> getExcludedDirectories() {
        Set<File> dirs = new HashSet<File>();
                
        return dirs;
    }
}
