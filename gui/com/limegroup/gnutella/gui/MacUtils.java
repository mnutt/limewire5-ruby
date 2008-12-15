package com.limegroup.gnutella.gui;

import java.awt.FileDialog;

import net.roydesign.ui.FolderDialog;

/**
 * A collection of OSX GUI utilities.
 *
 * This is in a separate class so that we won't have classloading errors if
 * OSX jars aren't included with other installations.
 */
public final class MacUtils {
    
    private MacUtils() {}
    
    /**
     * Returns the OSX Folder Dialog.
     */
    public static FileDialog getFolderDialog() {
        // net.roydesign.ui.FolderDialog:
        // This class takes advantage of a little know trick in 
        // Apple's VMs to show a real folder dialog, with a 
        // Choose button and all.
        return new FolderDialog(GUIMediator.getAppFrame(), "");
    }
}