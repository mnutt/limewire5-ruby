package com.limegroup.gnutella.gui;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Locale;

/**
 * This class constructs an <tt>Initializer</tt> instance that constructs
 * all of the necessary classes for the application.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class Main {
	
	/** 
	 * Creates an <tt>Initializer</tt> instance that constructs the 
	 * necessary classes for the application.
	 *
	 * @param args the array of command line arguments
	 */
	@SuppressWarnings("unchecked")
    public static void main(String args[]) {
	    Frame splash = null;
	    try {
            if (isMacOSX()) {
                // Register GURL to receive AppleEvents, such as magnet links.
                // Use reflection to not slow down non-OSX systems.
                // "GURLHandler.getInstance().register();"
                Class clazz = Class.forName("com.limegroup.gnutella.gui.GURLHandler");
                Method getInstance = clazz.getMethod("getInstance", new Class[0]);
                Object gurl = getInstance.invoke(null, new Object[0]);
                Method register = gurl.getClass().getMethod("register", new Class[0]);
                register.invoke(gurl, new Object[0]);
            }
            
			// show initial splash screen only if there are no arguments
            if (args == null || args.length == 0)
				splash = showInitialSplash();

            // load the GUI through reflection so that we don't reference classes here,
            // which would slow the speed of class-loading, causing the splash to be
            // displayed later.
            Class.forName("com.limegroup.gnutella.gui.GUILoader").
                getMethod("load", new Class[] { String[].class, Frame.class }).
                    invoke(null, new Object[] { args, splash });
        } catch(Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
	/**
	 * Shows the initial splash window.
	 */
	private static Frame showInitialSplash() {
	    Frame splashFrame = null;
        Image image = null;
        URL imageURL = ClassLoader.getSystemResource("org/limewire/gui/images/splash.png");
        if (imageURL != null) {
            image = Toolkit.getDefaultToolkit().createImage(imageURL);
            if (image != null) {
                splashFrame = AWTSplashWindow.splash(image);
            }
        }

	        
	    return splashFrame;
    }
    
    /** Determines if this is running on OS X. */
    private static boolean isMacOSX() {
        String os = System.getProperty("os.name").toLowerCase(Locale.US);
        return os.startsWith("mac os") && os.endsWith("x"); // Why not indexOf("mac os x") ?
    }
}
