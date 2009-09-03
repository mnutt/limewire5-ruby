package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final List<File> userDirectories;
    
    private static final List<String> sensitiveDirectories;

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

        userDirectories = getUserDirectories();
        sensitiveDirectories = getSensitiveDirectories();
    }
    
    /**
     * Builds list of directories with a given name for each user, starting from thier home directory. 
     */
    private static List<String> getUserDirectories(String directoryName) {
        List<String> userSubDirectories = new ArrayList<String>();
        for(File userHome : userDirectories) {
            File folder = new File(userHome, directoryName);
            userSubDirectories.add(folder.getAbsolutePath());
        }
        return userSubDirectories;
    }
    
    /**
     * Returns list of sensitive directories. 
     */
    private static List<String> getSensitiveDirectories() {
        List<String> sensitiveDirectories = new ArrayList<String>();
        
        if (OSUtils.isWindows()) {
            sensitiveDirectories.addAll(getUserDirectories("Documents"));
            sensitiveDirectories.addAll(getUserDirectories("My Documents"));
            sensitiveDirectories.addAll(getUserDirectories("Desktop"));
            sensitiveDirectories.add(File.separator + "Documents and Settings");
            sensitiveDirectories.add(File.separator + "Program Files");
            sensitiveDirectories.add(File.separator + "Windows");
            sensitiveDirectories.add(File.separator + "WINDOWS");
            sensitiveDirectories.add(File.separator + "WINNT");
            sensitiveDirectories.add(File.separator + "Users");
            sensitiveDirectories.add(File.separator + "Local Settings");
            sensitiveDirectories.add(File.separator + "Application Data");
            sensitiveDirectories.add(File.separator + "Temp");
            sensitiveDirectories.add(File.separator + "Temporary Internet Files");
        }

        if (OSUtils.isMacOSX()) {
            sensitiveDirectories.addAll(getUserDirectories("Documents"));
            sensitiveDirectories.addAll(getUserDirectories("Desktop"));
            sensitiveDirectories.addAll(getUserDirectories("Library"));
            sensitiveDirectories.add(File.separator + "Users");
            sensitiveDirectories.add(File.separator + "System");
            sensitiveDirectories.add(File.separator + "System Folder");
            sensitiveDirectories.add(File.separator + "Previous Systems");
            sensitiveDirectories.add(File.separator + "private");
            sensitiveDirectories.add(File.separator + "Volumes");
            sensitiveDirectories.add(File.separator + "Desktop");
            sensitiveDirectories.add(File.separator + "Applications");
            sensitiveDirectories.add(File.separator + "Applications (Mac OS 9)");
            sensitiveDirectories.add(File.separator + "Network");
        }

        if (OSUtils.isPOSIX()) {
            sensitiveDirectories.addAll(getUserDirectories("Desktop"));
            sensitiveDirectories.addAll(getUserDirectories("Documents"));
            sensitiveDirectories.add(File.separator + "bin");
            sensitiveDirectories.add(File.separator + "boot");
            sensitiveDirectories.add(File.separator + "dev");
            sensitiveDirectories.add(File.separator + "etc");
            sensitiveDirectories.add(File.separator + "home");
            sensitiveDirectories.add(File.separator + "mnt");
            sensitiveDirectories.add(File.separator + "opt");
            sensitiveDirectories.add(File.separator + "proc");
            sensitiveDirectories.add(File.separator + "root");
            sensitiveDirectories.add(File.separator + "sbin");
            sensitiveDirectories.add(File.separator + "usr");
            sensitiveDirectories.add(File.separator + "var");
        }
        return sensitiveDirectories;
    }

    /**
     * Returns list of possible user home directories. 
     */
    private static List<File> getUserDirectories() {
        File userHome = new File(System.getProperty("user.home"));
        List<File> userDirectories = new ArrayList<File>();
        userDirectories.add(userHome.getAbsoluteFile());
        
        File commonHome = null;
        if(OSUtils.isWindowsVista() || OSUtils.isWindows7()) {
            commonHome = new File("C:/Users");
        } else if(OSUtils.isWindows()) {
            commonHome = new File("C:/Documents and Settings");
        } else if( OSUtils.isMacOSX()) {
            commonHome = new File("/Users");
        } else if (OSUtils.isLinux()) {
            commonHome = new File("/home");
        }       
        
        //not all users will have their home directory under the commonHome, some might have custom locations.
        
        if(commonHome != null) {
            File[] directories =  commonHome.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            if(directories != null) {
            	userDirectories.addAll(Arrays.asList(directories));
            }
        }
        
        return userDirectories;
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
        if (folder == null) {
            return false;
        }

        if(userDirectories.contains(folder)) {
            return true;
        }
        
        String folderPath = folder.getPath();
        for (String name : sensitiveDirectories) {
            if (folderPath.endsWith(name)) {
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
