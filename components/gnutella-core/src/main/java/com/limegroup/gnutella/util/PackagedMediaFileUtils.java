package com.limegroup.gnutella.util;

import java.io.File;

import org.limewire.io.Expand;
import org.limewire.util.Base32;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 *  Support methods for the unpacking and launching of pmf file types.
 *
 *  See PackagedMediaFileLauncher in the gui for more information.
 */
public class PackagedMediaFileUtils {

    /**
     *  Ensure that the PMF File is properly expanded and return the index file.
     */
    public static File preparePMFFile(String fname) {
        // Ensure there is a temp dir
        String home  = System.getProperty("user.home");
        String temp  = home + File.separator + ".temp";
        File   ftemp = new File(temp);
        ftemp.mkdir();

        // Ensure the file exists
        File   ffname = new File(fname);
        if ( !ffname.exists() )
            return null;

        // Ensure there is a file specific unpack dir
        String file  = temp + File.separator + getUnpackDirectory(ffname);
        File   ffile = new File(file);
        ffile.mkdir();

        File lfile = null;
        try {
            // If the file is already unpacked then don't bother unpacking
            lfile = createIndexFileHandle(ffile);
            if ( !lfile.exists() ) {
                lfile = null;
                Expand.expandFile(ffname, ffile); 

                // Get the index file from unpacked directory
                lfile = createIndexFileHandle(ffile);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return lfile;
    }


    /**
     *  Look to see if there is an index.htm? file available
     */
    private static File createIndexFileHandle(File dir) {
        File   lfile = new File(dir, "index.html");
        if ( !lfile.exists() ) {
            lfile = new File(dir, "index.htm");
        }
        return lfile;
    }

    /**
     *  Create the unpack directory and its name based on file contents
     */
    private static String getUnpackDirectory(File pmfFile) {

        // Compute a quick hash of the file for added uniqueness
        // Use the first 6 excoded chars of the hash as the end of the 
        // unpack directory name
        String hash;
        try {
            byte hbytes[] = LimeXMLUtils.hashFile(pmfFile);
            if (hbytes == null || hbytes.length <= 0 )
                throw new Exception(); 
            hash = Base32.encode(hbytes);
            hash = hash.substring(0,6);
        } catch (Exception hashFailed) {
            hash = "";
        }
        // Limit Long file names for temp unpack directory
        String fullname = pmfFile.getName();
        if ( fullname.length() > 24 )
            fullname = fullname.substring(0,24);  

        return fullname + hash;
    }
}

