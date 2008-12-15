package com.limegroup.gnutella.gui.sharing;

import java.io.File;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.library.LibraryUtils;

/**
 * Utility to contain all share-related classes.
 */
// TODO: Convert this to an instance & pass in the FileManager & ActivityCallback.
public class ShareManager {
    
    /**
     * Returns true if the given folder can be shared.
     * If it can't be, this will display a warning about why it isn't.
     */
    public static boolean checkAndWarnNewSharedFolder(File folder) {
        // User chose nothing, exit w/o any warnings.
        if(folder == null)
            return false;
        
        // Ensure readable.
        if(!folder.isDirectory() || !folder.canRead()) {
            GUIMediator.showError(I18n.tr("<html><table width=400>You selected: <i>{0}</i><br>LimeWire cannot share this folder because it is either not a folder or cannot be read.<br><br>Please select another folder to share.</table></html>", folder));
            return false;
        }
        
        // Ensure not banned.
//        if (!GuiCoreMediator.getFileManager().isFolderShareable(folder, false)) {
//            GUIMediator.showError(I18n.tr("<html><table width=400>You selected: <i>{0}</i><br>LimeWire cannot share this folder for security reasons.<br><br>Please select another folder to share.</table></html>", folder));
//            return false;
//        }
        
        // Double-check sensitive directories. 
        if (LibraryUtils.isSensitiveDirectory(folder) && !warnAboutSensitiveDirectory(folder)) {
            return false;
        }
        
        return true;
    }

    public static boolean warnAboutSensitiveDirectory(File folder) {
        return true;
        //  Use unicode char for non-breaking space so that
        //  directory name is listed all on one line.
//        String dirName = GUIUtils.convertToNonBreakingSpaces(0, folder.getAbsolutePath());
//        DialogOption retval = GUIMediator.showYesNoMessage(
//                I18n.tr("<html><table width=400>The folder <i>{0}</i><br>is likely to contain sensitive or personal information.<br><br>LimeWire recommends that you <b>DO NOT</b> share this folder.<br><br>Are you sure you want to share it?</table></html>", dirName),
//                DialogOption.NO);
//        if (retval == DialogOption.YES) {
//            GuiCoreMediator.getFileManager().validateSensitiveFile(folder);
//            return true;
//        } else {
//            GuiCoreMediator.getFileManager().invalidateSensitiveFile(folder);
//            return false;
//        }
    }

}
