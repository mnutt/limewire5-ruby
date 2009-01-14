package org.limewire.ui.swing.wizard;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.library.LibraryData;
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
    private static void addIfSupported(LibraryData data, String path, Set<File> folders) {
        if (path != null && !path.isEmpty()) {
            addIfAllowed(data, new File(path), folders);
        }
    }
    
    private static void addIfAllowed(LibraryData data, File folder, Collection<File> folders) {
        if(data.isDirectoryAllowed(folder)) {
            folders.add(folder);
        }
    }
    
    /**
     * Determines the OS specific list of directories to manage by default.
     */
    public static Set<File> getDefaultManagedDirectories(LibraryData data) {
        Set<File> dirs = new HashSet<File>();

        if (OSUtils.isWindows() && !OSUtils.isWindowsVista()) {
            addIfSupported(data, SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS), dirs);
            addIfSupported(data, SystemUtils.getSpecialPath(SpecialLocations.DESKTOP), dirs);
        } else {
            addIfSupported(data, SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS), dirs);
            addIfSupported(data, SystemUtils.getSpecialPath(SpecialLocations.DESKTOP), dirs);
            
            String homePath = SystemUtils.getSpecialPath(SpecialLocations.HOME);            
            if (homePath != null && !homePath.isEmpty()) {
                addIfAllowed(data, new File(homePath, "Downloads"), dirs);
                addIfAllowed(data, new File(homePath, "Music"), dirs);
                addIfAllowed(data, new File(homePath, "Pictures"), dirs);
                
                if (OSUtils.isWindowsVista()) { 
                    addIfAllowed(data, new File(homePath, "Videos"), dirs);
                } 
                else {
                    addIfAllowed(data, new File(homePath, "Public"), dirs);
                    addIfAllowed(data, new File(homePath, "Movies"), dirs);
                }
            }
        }
                
        return dirs;
    }
}
