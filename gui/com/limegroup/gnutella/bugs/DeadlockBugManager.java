package com.limegroup.gnutella.bugs;

import org.limewire.core.settings.BugSettings;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;

import com.google.inject.Inject;
import com.limegroup.gnutella.gui.LocalClientInfoFactory;
import com.limegroup.gnutella.util.LimeWireUtils;

public class DeadlockBugManager {

   @Inject private static volatile LocalClientInfoFactory localClientInfoFactory;

   private DeadlockBugManager() {}
    
    /** Handles a deadlock bug. */
    public static void handleDeadlock(DeadlockException bug, String threadName, String message) {
        bug.printStackTrace();
        System.err.println("Detail: " + message);
        
        LocalClientInfo info = localClientInfoFactory.createLocalClientInfo(bug, threadName, message, false);
        // If it's a sendable version & we're either a beta or the user said to send it, send it
        if(isSendableVersion() && (LimeWireUtils.isBetaRelease() || BugSettings.SEND_DEADLOCK_BUGS.getValue()))
            sendToServlet(info);
    }
    
    /** Determines if we're allowed to send a bug report. */
    private static boolean isSendableVersion() {
        Version myVersion;
        Version lastVersion;
        try {
            myVersion = new Version(LimeWireUtils.getLimeWireVersion());
            lastVersion = new Version(BugSettings.LAST_ACCEPTABLE_VERSION.getValue());
        } catch(VersionFormatException vfe) {
            return false;
        }
        
        return myVersion.compareTo(lastVersion) >= 0;
    }
    
    private static void sendToServlet(LocalClientInfo info) {
        new ServletAccessor(false).getRemoteBugInfo(info);
    }
}
