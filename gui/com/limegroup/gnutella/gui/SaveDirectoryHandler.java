package com.limegroup.gnutella.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.settings.QuestionsHandler;
import org.limewire.core.settings.SharingSettings;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.library.LibraryUtils;

/** Handles prompting the user to enter a valid save directory. */
public final class SaveDirectoryHandler {   

    /** Ensure that this class cannot be constructed from outside this class. */
    private SaveDirectoryHandler() {} 
    
    public static enum ValidationResult {
        VALID, BAD_PERMS, BAD_VISTA, BAD_BANNED, BAD_SENSITIVE;
    }


    /**
     * Ensures that the current save directory is valid,
     * prompting for a new one if it isn't.
     */
    public static void validateSaveDirectoryAndPromptForNewOne() {    
        File saveDir = SharingSettings.getSaveDirectory();
        if(!isSaveDirectoryValid(saveDir) || !showVistaWarningIfNeeded(saveDir))
            promptAndSetNewSaveDirectory();
    }
    
    /** Ensures that the given folder is valid for a save directory. */
    public static ValidationResult isFolderValidForSaveDirectory(File folder) {
        if(!checkForBannedDirectory(folder))
            return ValidationResult.BAD_BANNED;
        if(!isSaveDirectoryValid(folder))
            return ValidationResult.BAD_PERMS;
        if(!showVistaWarningIfNeeded(folder))
            return ValidationResult.BAD_VISTA;
        if(!checkForSensitiveDirectory(folder))
            return ValidationResult.BAD_SENSITIVE;
        
        return ValidationResult.VALID;
    }
    
    /**
     * Determines if the folder is totally banned.
     */
    private static boolean checkForBannedDirectory(File folder) {
        if(LibraryUtils.isFolderBanned(folder)) {
            GUIMediator.showError(I18n.tr("<html><table width=400>You selected: <i>{0}</i><br>LimeWire cannot use this as a save folder for security reasons.<br><br>Please select another save folder.</table></html>", folder));
            return false;
        } else {
            return true;
        }
            
    }
    
    /**
     * Determines if the folder is potentially sensitive.
     */
    private static boolean checkForSensitiveDirectory(File folder) {
        if(!LibraryUtils.isSensitiveDirectory(folder)) {
            SharingSettings.LAST_WARNED_SAVE_DIRECTORY.setValue("");
            return true;
        }
        
        if(SharingSettings.LAST_WARNED_SAVE_DIRECTORY.equals(folder.getPath()))
            return true;
        
        //  Use unicode char for non-breaking space so that
        //  directory name is listed all on one line.
        String dirName = GUIUtils.convertToNonBreakingSpaces(0, folder.getAbsolutePath());
        DialogOption retval = GUIMediator.showYesNoMessage(
                I18n.tr("<html><table width=400>The folder <i>{0}</i><br>is likely to contain sensitive or personal information.<br><br>LimeWire recommends that you <b>choose another</b> folder for saving your downloads.<br><br>Are you sure you want to download to this folder?</table></html>", dirName),
                DialogOption.NO);
        if(retval == DialogOption.YES) {
            SharingSettings.LAST_WARNED_SAVE_DIRECTORY.setValue(folder.getPath());
            return true;
        } else {
            SharingSettings.LAST_WARNED_SAVE_DIRECTORY.setValue("");
            return false;
        }
    }

    /**
     * Constructs a new window that prompts the user to enter a valid save
     * directory.
     *
     * This doesn't return until the user has chosen a valid directory.
     */
    private static void promptAndSetNewSaveDirectory() {
        File dir = null;
        while(!isSaveDirectoryValid(dir) || !showVistaWarningIfNeeded(dir)) {
            final AtomicReference<File> dirRef = new AtomicReference<File>();
            GUIMediator.safeInvokeAndWait(new Runnable() {
                public void run() {
                    GUIMediator.showError(I18n.tr("Your save folder is not valid. It may have been deleted, you may not have permissions to write to it, or there may be another problem. Please choose a different folder."));
                    dirRef.set(showChooser());
                }
            });
            dir = dirRef.get();
            if(dir == null)
                continue;
            FileUtils.setWriteable(dir);
        }
    }

    /**
     * Shows the chooser & sets the save directory setting, adding the save
     * directory as shared, also.
     *
     * @return the selected <tt>File</tt>, or <tt>null</tt> if there were
     *  any problems
     */
    private static File showChooser() {
        final AtomicReference<File> dirRef = new AtomicReference<File>();
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                dirRef.set(FileChooserHandler.getInputDirectory(null));                
            }
        });
        
        File dir = dirRef.get();
        if(dir != null) {
            try {
                // updates Incomplete directory etc... 
                SharingSettings.setSaveDirectory(dir);
                //SharingSettings.DIRECTORIES_TO_SHARE.add(dir);
                return dir;
            } catch(IOException ignored) {}
        }
        return null;
    }
    
    /**
     * Utility method for checking whether or not the save directory is valid.
     * 
     * @param saveDir the save directory to check for validity
     * @return <tt>true</tt> if the save directory is valid, otherwise 
     *  <tt>false</tt>
     */
    private static boolean isSaveDirectoryValid(File saveDir) {
        if(saveDir == null || saveDir.isFile()) 
            return false;

        if(!saveDir.exists())
            saveDir.mkdirs();
        
        if(!saveDir.isDirectory())
            return false;
        
        FileUtils.setWriteable(saveDir);
        
        Random generator = new Random();
        File testFile = null;
        for(int i = 0; i < 10 && testFile == null; i++) {
            StringBuilder name = new StringBuilder();
            for(int j = 0; j < 8; j++) {
                name.append((char)('a' + generator.nextInt('z'-'a')));
            }
            name.append(".tmp");
            
            testFile = new File(saveDir, name.toString());
            if (testFile.exists()) {
                testFile = null; // try again!
            }
        }
        
        if (testFile == null) {
            return false;
        }
        
        RandomAccessFile testRAFile = null;
        try {
            testRAFile = new RandomAccessFile(testFile, "rw");
         
            // Try to write something just to make extra sure we're OK.
            testRAFile.write(7);
            testRAFile.close();
        } catch (FileNotFoundException e) {
            // If we could not open the file, then we can't write to that 
            // directory.
            return false;
        } catch(IOException e) {
            // The directory is invalid if there was an error writing to it.
            return false;
        } finally {
            // Delete our test file.
            testFile.delete();
            try {
                if(testRAFile != null)
                    testRAFile.close();
            } catch (IOException ignored) {}
        }
        
        return FileUtils.canWrite(saveDir);
    }
    
    private static boolean isGoodVistaDirectory(File f) {
        if (!OSUtils.isWindowsVista())
            return true;
        try {
            return FileUtils.isReallyInParentPath(CommonUtils.getUserHomeDir(), f);
        } catch (IOException iox) {
            return true; // probably bad, but not vista-specific
        }
    }
    
    /**
     * @param f the directory the user wants to save to
     * @return true if its ok to use that directory
     */
    private static boolean showVistaWarningIfNeeded(File f) {
        if (isGoodVistaDirectory(f))
            return true;
        return GUIMediator
                .showYesNoMessage(I18n.tr("Saving downloads to {0} may not function correctly.  To be sure downloads are saved properly you should save them to a sub-folder of {1}.  Would you like to choose another location?",
                                        f, CommonUtils.getUserHomeDir()),
                        QuestionsHandler.VISTA_SAVE_LOCATION, DialogOption.YES
                ) == DialogOption.NO;
    }
}

