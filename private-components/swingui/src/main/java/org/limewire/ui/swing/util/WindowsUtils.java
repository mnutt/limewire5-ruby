package org.limewire.ui.swing.util;

import java.io.File;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;


/**
 * A collection of Windows-related GUI utility methods.
 */
public class WindowsUtils {
    
    private WindowsUtils() {}

    /**
     * Determines if we know how to set the login status.
     */    
    public static boolean isLoginStatusAvailable() {
        return OSUtils.isGoodWindows();
    }

    /**
     * Sets the login status.  Only available on W2k+.
     */
    public static void setLoginStatus(boolean allow) {
        if(!isLoginStatusAvailable())
            return;
        
        
        File src = new File("LimeWire On Startup.lnk");
        File homeDir = CommonUtils.getUserHomeDir();
        File startup = new File(homeDir, "Start Menu\\Programs\\Startup");
        File dst = new File(startup, "LimeWire On Startup.lnk");
        
        if(allow)
            FileUtils.copy(src, dst);
        else
            dst.delete();
    }
}
