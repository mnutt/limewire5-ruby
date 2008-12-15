package com.limegroup.gnutella.gui.swingbrowser;

import java.lang.reflect.Field;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.WindowCreator;
import org.mozilla.interfaces.nsIWindowWatcher;

public class WinCreatorHook {
    
    private static final Log LOG = LogFactory.getLog(WinCreatorHook.class);
    
    private WinCreatorHook() {}

    public static void addHook() {
        // Inform the XPCOM layer about the custom WinCreator.
        // Do this within mozAsyncExec, since we need to wait till after Moz init has finished.
        MozillaExecutor.mozAsyncExec(new Runnable() {
            public void run() {
                final WindowCreator creator = new LimeWindowCreator(MozillaInitialization.getWinCreator());
                nsIWindowWatcher winWatcher = XPCOMUtils.getService("@mozilla.org/embedcomp/window-watcher;1", nsIWindowWatcher.class);
                winWatcher.setWindowCreator(creator);
                // Set the creator also in the MozillaInitialization, so newly created
                // frames callback to the right class.
                // Do this in the AWT Thread, since that's the only thread that calls
                // MozillaInitialization.getWinCreator().
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            Field field = MozillaInitialization.class.getDeclaredField("winCreator");
                            field.setAccessible(true);
                            field.set(null, creator);
                        } catch(Throwable t) {
                            LOG.debug("Unable to set WinCreator on MozillaInitialization", t);
                        }
                    }
                });
            }
        });
    }
}
