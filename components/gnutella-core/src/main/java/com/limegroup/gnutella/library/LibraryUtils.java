package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;

public class LibraryUtils {
    
    // TODO: refactor back into filemanger for test mocking... use instances.
    
    /** Subdirectory that is always shared. */
    public static final File PROGRAM_SHARE;

    /** Subdirectory that also is always shared. */
    public static final File PREFERENCE_SHARE;

    /** Subdirectory used to share special application files. */
    public static final File APPLICATION_SPECIAL_SHARE;

    static {
        File forceShare = new File(".", ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PROGRAM_SHARE = forceShare;
        
        forceShare = 
            new File(CommonUtils.getUserSettingsDir(), ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PREFERENCE_SHARE = forceShare;
        
        forceShare = 
            new File(CommonUtils.getUserSettingsDir(), ".AppSpecialShare").getAbsoluteFile();
        forceShare.mkdir();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        APPLICATION_SPECIAL_SHARE = forceShare;
    }

    /** 
     * @return <code>isFilePhysicallyManagable(file) && isFileAllowedToBeManaged(file))</code>
     */
    public static boolean isFileManagable(File file) {
        return isFilePhysicallyManagable(file) && isFileAllowedToBeManaged(file);
    }

    /**
     * Returns true if this file is not too large, not too small,
     * not null, not a directory, not unreadable, not hidden.
     * <p>
     * Returns false otherwise.
     */
    public static boolean isFilePhysicallyManagable(File file) {
        if (file == null || !file.exists() || file.isDirectory() || !file.canRead() || file.isHidden() ) { 
            return false;
        }

        long fileLength = file.length();
        if (fileLength <= 0 || fileLength > MAX_FILE_SIZE)  {
            return false;
        }

        return true;
    }

    /**
     * If managing programs is disabled and the specified file is not a forced
     * share, returns false if the file is a program. Otherwise returns false
     * if the file's extension is banned. Otherwise returns true.
     */
    public static boolean isFileAllowedToBeManaged(File file) {
        String ext = FileUtils.getFileExtension(file).toLowerCase(Locale.US);
        if(!LibrarySettings.ALLOW_PROGRAMS.getValue() && !LibraryUtils.isForcedShare(file)) {
            MediaType type = MediaType.getMediaTypeForExtension(ext);
            if(type == MediaType.getProgramMediaType())
                return false;
        }
        
// TODO: This generated a small # of complaints (and broke some tests, but not hard to fix those)
//       Before re-adding, should confirm that we definitely want this behavior, and maybe 
//       turn it into an option?  The UI right now exposes these extensions as
//       "banned search result extensions", which is a little different than what this code is doing.
        
//        String dotExt = "." + ext;
//        for(String banned : FilterSettings.BANNED_EXTENSIONS.get()) {
//            if(banned.equals(dotExt))
//                return false;
//        }
        return true;    
    }
    
    
    /**
     * Returns true iff <tt>file</tt> is a sensitive directory.
     */
    public static boolean isSensitiveDirectory(File folder) {
        if (folder == null)
            return false;

        String userHome = System.getProperty("user.home");
        if (folder.equals(new File(userHome)))
            return true;
        
        String userHomeShortDir = userHome.substring(userHome.lastIndexOf(File.separator)+1);

        String[] sensitive;
        if (OSUtils.isWindowsVista()) {
            //Windows Vista does not have the "My" prefix for the "My Documents" folder.  It is simply "Documents"
            // TODO: Are Local Settings & everything after that right for Vista?
            sensitive = new String[] { "Documents and Settings",
                    userHomeShortDir + File.separator + "Documents", "Desktop", "Program Files",
                    "Windows", "WINNT", "Users", "Local Settings", "Application Data", "Temp",
                    "Temporary Internet Files" };
        } else if (OSUtils.isWindows()) {
            sensitive = new String[] { "Documents and Settings", "My Documents", "Desktop",
                    "Program Files", "Windows", "WINNT", "Users", "Local Settings",
                    "Application Data", "Temp", "Temporary Internet Files" };
        } else if (OSUtils.isMacOSX()) {
            sensitive = new String[] { "Users",
                    userHomeShortDir + File.separator + "Documents", "System", "System Folder", "Previous Systems",
                    "private", "Volumes", "Desktop", "Applications", "Applications (Mac OS 9)",
                    "Network" };
        } else if (OSUtils.isPOSIX()) {
            sensitive = new String[] { "bin", userHomeShortDir + File.separator + "Documents", "boot", "dev", "etc", "home", "mnt", "opt", "proc",
                    "root", "sbin", "usr", "var" };
        } else {
            sensitive = new String[0];
        }

        String folderPath = folder.getPath();
        for (String name : sensitive) {
            if (folderPath.endsWith(File.separator + name)){
                return true;
            }
        }

        return false;
    }
    
    /**
     * Determines if this FileDesc is a network share.
     */
    public static boolean isForcedShare(FileDesc desc) {
        return isForcedShare(desc.getFile());
    }
    
    /**
     * Determines if this File is a network share.
     */
    public static boolean isForcedShare(File file) {
        File parent = file.getParentFile();
        return parent != null && isForcedShareDirectory(parent);
    }
    
    /**
     * Determines if this File is an application special share.
     */
    public static boolean isApplicationSpecialShare(File file) {
        File parent = file.getParentFile();
        return parent != null && isApplicationSpecialShareDirectory(parent);
    }
    
    /**
     * Determines if this File is a network shared directory.
     */
    public static boolean isForcedShareDirectory(File f) {
        return f != null && (f.equals(LibraryUtils.PROGRAM_SHARE) || f.equals(LibraryUtils.PREFERENCE_SHARE));
    }
    
    public static boolean isApplicationSpecialShareDirectory(File directory) {
        return directory.equals(LibraryUtils.APPLICATION_SPECIAL_SHARE);
    }

    /**
     * Returns true if this folder should never be a managed.
     */
    public static boolean isFolderBanned(File folder) {        
        //  check for system roots
        File[] faRoots = File.listRoots();
        if (faRoots != null && faRoots.length > 0) {
            for (int i = 0; i < faRoots.length; i++) {
                if (folder.equals(faRoots[i]))
                    return true;
            }
        }
        
        // Check for the folder name being 'Cookies', or 'Cookies\Low' [vista]
        // TODO: Make sure this is i18n-safe
        String name = folder.getName().toLowerCase(Locale.US);
        if(name.equals("cookies"))
            return true;
        else if(name.equals("low")) {
            String parent = folder.getParent();
            if(parent != null && parent.toLowerCase(Locale.US).equals("cookies"))
                return true;
        }
        
        return false;
    }
    
}
