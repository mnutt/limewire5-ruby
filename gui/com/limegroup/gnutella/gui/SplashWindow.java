package com.limegroup.gnutella.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;

import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * Window that displays the splash screen.  This loads the splash screen
 * image, places it on the center of the screen, and allows dynamic
 * updating of the status text for loading the application.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SplashWindow {

    /** The sole instance of the SplashWindow */
    private static SplashWindow INSTANCE;
    
    private AtomicBoolean initialized = new AtomicBoolean(false);
    
    /**
     * Constant handle to the glass pane that handles drawing text
     * on top of the splash screen.
     */
    private volatile StatusComponent glassPane;

    /**  Constant handle to the label that represents the splash image. */
    private volatile JLabel splashLabel;
    
    /** The JWindow the splash uses. */
    private volatile JWindow splashWindow;

    /** Returns the single instance of the SplashWindow. */
    public static synchronized SplashWindow instance() {
        if(INSTANCE == null) {
            INSTANCE = new SplashWindow();
        }
	    return INSTANCE;
    }    
    
    /** Determines if the splash is constructed. */
    public static synchronized boolean isSplashConstructed() {
        return INSTANCE != null;
    }
    
    private void initialize() {
        glassPane = new StatusComponent(15);
        splashLabel = new JLabel();
        splashWindow = new JWindow();
        
        glassPane.add(Box.createVerticalGlue(), 0);
        glassPane.add(Box.createVerticalStrut(8));
        glassPane.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        
        String name = LimeWireUtils.isPro() ? "splashpro" : "splash";
        URL imageURL = ClassLoader.getSystemResource("org/limewire/gui/images/" + name + ".png");
        assert imageURL != null;
        Image splashImage = Toolkit.getDefaultToolkit().createImage(imageURL);
        // Load the image
        MediaTracker mt = new MediaTracker(splashWindow);
        mt.addImage(splashImage,0);
        try {
            mt.waitForID(0);
        } catch(InterruptedException ie){}

        int imgWidth = splashImage.getWidth(null);
        if(imgWidth < 1)
            imgWidth = 1;
        int imgHeight = splashImage.getHeight(null);
        if(imgHeight < 1)
            imgHeight = 1;
        Dimension size = new Dimension(imgWidth + 2, imgHeight + 2);
        splashWindow.setSize(size);        
  
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        splashWindow.setLocation((screenSize.width - size.width) / 2, (screenSize.height - size.height) / 2);
        splashLabel.setIcon(new ImageIcon(splashImage));
        splashWindow.getContentPane().add(splashLabel, BorderLayout.CENTER);

        splashWindow.setGlassPane(glassPane);
        splashWindow.pack();
    }
    
    /**
     * Sets the Splash Window to be visible.
     */
    public void begin() {
        if(initialized.getAndSet(true)) 
            return;
        
        runLater(new Runnable() {
            public void run() {
                initialize();
                splashWindow.toFront();
                splashWindow.setVisible(true);
                glassPane.setVisible(true);
                setStatusText(I18n.tr("Loading LimeWire..."));
            }
        });
    }

    /**
     * Sets the loading status text to display in the splash 
     * screen window.
     *
     * @param text the text to display
     */
    public void setStatusText(final String text) {
        runLater(new Runnable() {
            public void run() {
                glassPane.setText(text);
                // force a redraw so the status is shown immediately,
                // even if we're currently in the Swing thread.
                glassPane.paintImmediately(0, 0, glassPane.getWidth(), glassPane.getHeight());
            }
        });
    }

    /**
     * Refreshes the image on the SplashWindow based on the current theme.
     * This method is used primarily during theme change.
     */
    public void refreshImage() {
        runLater(new Runnable() {
            public void run() {
            	final ImageIcon splashIcon = ResourceManager.getThemeImage("splash");
            	splashLabel.setIcon(splashIcon);
            	glassPane.setVisible(false);
            	splashWindow.pack();
            	//  force redraw so that splash is drawn before rest of theme changes
            	splashLabel.paintImmediately(0, 0, splashLabel.getWidth(), splashLabel.getHeight());
            }
        });
    }
    
    private void runLater(Runnable runner) {
        if(initialized.get())
            GUIMediator.safeInvokeAndWait(runner);
    }

    public void dispose() {
        runLater(new Runnable() {
            public void run() {
                splashWindow.dispose();
            }
        });
    }

    public boolean isShowing() {
        return splashWindow != null && splashWindow.isShowing();
    }

    public void setVisible(final boolean b) {
        runLater(new Runnable() {
            public void run() {
                splashWindow.setVisible(b);
            }
        });
    }

    public void toBack() {
        runLater(new Runnable() {
            public void run() {
                splashWindow.toBack();
            }
        });
    }
}

