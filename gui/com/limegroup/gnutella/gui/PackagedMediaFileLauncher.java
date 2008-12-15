package com.limegroup.gnutella.gui;

import java.io.File;

import com.limegroup.gnutella.util.PackagedMediaFileUtils;

/**
 *  Allow pmf files to be launched externally from LimeWire.
 *
 *  If LimeWire is started with "-pmf filename" arguments, 
 *  then the file will be unpacked and a root level index.htm[l] 
 *  will be launched in the browser.  Launching a ".pmf" file from
 *  the Library will have the same effect.
 * 
 *  pmf files are really just zip files with top level index.html files.
 */
public class PackagedMediaFileLauncher {

    // Launch a pmf file directly
//    public static void main(String args[]) {
//        if ( args.length >= 1 ) {
//            launchFile(args[0], false);
//        }
//    }

    /**
     *  Take a full path name 
     */
    public static void launchFile(String fname, boolean isLimeRunning) {
        // Load libraries if running from command line
        if ( ! isLimeRunning )
            ResourceManager.instance();
        File lfile = PackagedMediaFileUtils.preparePMFFile(fname);

        // Don't launch an invalid file
        if ( lfile == null )
            return;

        GUIMediator.launchFile(lfile);
    }
}

