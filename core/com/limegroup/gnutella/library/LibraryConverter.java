package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;


/**
 * Converts from old-style library settings (shared files)
 * to new-style library settings (managed files)
 */
@SuppressWarnings("deprecation")
class LibraryConverter {
    
    boolean isOutOfDate() {
        return LibrarySettings.VERSION.getValue() == LibrarySettings.LibraryVersion.FOUR_X.name();
    }
    
    void convert(LibraryFileData newData) {
        newData.revertToDefault();
        
        OldLibraryData oldData = new OldLibraryData(); // load if necessary
        for(File folder : OldLibrarySettings.DIRECTORIES_TO_SHARE.getValue()) {
            if(!LibraryUtils.isSensitiveDirectory(folder) || oldData.SENSITIVE_DIRECTORIES_VALIDATED.contains(folder)) {
                folder = FileUtils.canonicalize(folder);
                newData.addDirectoryToManageRecursively(folder);
            }
        }
        
        newData.setDirectoriesToExcludeFromManaging(oldData.DIRECTORIES_NOT_TO_SHARE);
        
        for(File file : oldData.SPECIAL_FILES_TO_SHARE) {
            file = FileUtils.canonicalize(file);            
            newData.addManagedFile(file, true);
            newData.setSharedWithGnutella(file, true);
        }
        
        for(File file : oldData.FILES_NOT_TO_SHARE) {
            file = FileUtils.canonicalize(file);
            newData.removeManagedFile(file, true);
        }
        
        // Set the new managed extensions.
        List<String> extensions = new ArrayList<String>();
        extensions.addAll(Arrays.asList(OldLibrarySettings.getDefaultExtensions()));
        extensions.removeAll(Arrays.asList(StringArraySetting.decode(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue())));
        extensions.addAll(Arrays.asList(StringArraySetting.decode(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue())));        
        newData.setManagedExtensions(extensions);
        
        // Here's the bulk of the conversion -- loop through, recursively, previously 
        // shared directories & mark all potential files as shareable.
        convertSharedFiles(newData);
        
        
        for(File file : oldData.SPECIAL_STORE_FILES) {
            file = FileUtils.canonicalize(file);
            newData.addManagedFile(file, true);
        }
        
        for(MediaType type : MediaType.getDefaultMediaTypes()) {
            File file = SharingSettings.getFileSettingForMediaType(type).getValue();
            file = FileUtils.canonicalize(file);
            newData.addDirectoryToManageRecursively(file);
        }
        
        newData.addDirectoryToManageRecursively(FileUtils.canonicalize(SharingSettings.getSaveLWSDirectory()));
        
        newData.addDirectoryToManageRecursively(FileUtils.canonicalize(SharingSettings.getSaveDirectory()));
        
        LibrarySettings.VERSION.setValue(LibrarySettings.LibraryVersion.FIVE_0_0.name());
        
        oldData.revertToDefault();
        OldLibrarySettings.DIRECTORIES_TO_SHARE.revertToDefault();
        OldLibrarySettings.DISABLE_SENSITIVE.revertToDefault();
        OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.revertToDefault();
        OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.revertToDefault();
        OldLibrarySettings.EXTENSIONS_TO_SHARE.revertToDefault();
    }
    
    private void convertSharedFiles(LibraryFileData data) {
        Map<Category, Collection<String>> extByCategory = data.getExtensionsPerCategory();
        Collection<String> extensions = new ArrayList<String>();
        for(Collection<String> exts : extByCategory.values()) {
            extensions.addAll(exts);
        }
        Set<File> convertedDirectories = new HashSet<File>();
        for(File file : data.getDirectoriesToManageRecursively()) {
            convertDirectory(file, extensions, data, convertedDirectories);
        }
    }
    
    // This replicates a lot of the logic in ManagedFileListImpl for scanning for
    // managed files, but it's a lot simpler & exists only to upgrade prior versions.
    private void convertDirectory(File directory, final Collection<String> extensions,
            final LibraryFileData data, Set<File> convertedDirectories) {
        // If we already converted this directory, exit.
        if(convertedDirectories.contains(directory)) {
            return;
        }
        
        convertedDirectories.add(directory);
        File[] fileList = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return LibraryUtils.isFileManagable(file)
                        && extensions.contains(FileUtils.getFileExtension(file))
                        && !data.isFileExcluded(file);
            }
        });

        if(fileList != null) {
            for (File file : fileList) {
                file = FileUtils.canonicalize(file);
                data.setSharedWithGnutella(file, true);
            }
        }

        if(!LibraryUtils.isForcedShareDirectory(directory)) {
            File[] dirList = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File folder) {
                    return folder.isDirectory()
                            && folder.canRead()
                            && !data.isIncompleteDirectory(folder)
                            && !LibraryUtils.isApplicationSpecialShareDirectory(folder)
                            && !data.isFolderExcluded(folder)
                            && !LibraryUtils.isFolderBanned(folder);
                }
            });
            
            if(dirList != null) {
                for (File subdir : dirList) {
                    convertDirectory(subdir, extensions, data, convertedDirectories);
                }
            }
        }
    }
}
