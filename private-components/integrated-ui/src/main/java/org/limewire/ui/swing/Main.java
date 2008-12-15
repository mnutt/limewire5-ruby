package org.limewire.ui.swing;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Locale;

/** The entry point into the program using the real core. */
public class Main {
    
    public static void main(String[] args) {
        try {
            if (isMacOSX()) {
                // Register GURL to receive AppleEvents, such as magnet links.
                // Use reflection to not slow down non-OSX systems.
                // "GURLHandler.getInstance().register();"
                Class<?> clazz = Class.forName("org.limewire.ui.swing.GURLHandler");
                Method getInstance = clazz.getMethod("getInstance", new Class[0]);
                Object gurl = getInstance.invoke(null, new Object[0]);
                Method register = gurl.getClass().getMethod("register", new Class[0]);
                register.invoke(gurl, new Object[0]);
            }        
        
        
            Frame splash = null;
            Image splashImage = getSplashImage();
            
            // show initial splash screen only if there are no arguments
            if (args == null || args.length == 0) {
                if(splashImage != null) {
                    splash = AWTSplashWindow.splash(splashImage);
                }
            }
            
            // load the GUI through reflection so that we don't reference classes here,
            // which would slow the speed of class-loading, causing the splash to be
            // displayed later.
            Class<?> loadClass = Class.forName("org.limewire.ui.swing.GuiLoader");
            Object loadInstance = loadClass.newInstance();
            Method loadMethod = loadClass.getMethod("load", new Class[] { String[].class, Frame.class, Image.class } );
            loadMethod.invoke(loadInstance, args, splash, splashImage);
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Gets the image to be used as the splash.
     */
    private static Image getSplashImage() {
        URL imageURL = ClassLoader.getSystemResource("org/limewire/ui/swing/mainframe/resources/splash.png");
        if (imageURL != null) {
            return Toolkit.getDefaultToolkit().createImage(imageURL);
        } else {
            return null;
        }
    }

    /** Determines if this is running on OS X. */
    private static boolean isMacOSX() {
        return System.getProperty("os.name", "").toLowerCase(Locale.US).startsWith("mac os x");
    }
}
