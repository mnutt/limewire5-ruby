package com.limegroup.gnutella.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import net.roydesign.event.ApplicationEvent;
import net.roydesign.mac.MRJAdapter;

import com.limegroup.gnutella.browser.ExternalControl;

/**
 * This class handles Macintosh specific events. The handled events  
 * include the selection of the "About" option in the Mac file menu,
 * the selection of the "Quit" option from the Mac file menu, and the
 * dropping of a file on LimeWire on the Mac, which LimeWire would be
 * expected to handle in some way.
 */
public class MacEventHandler {
    
    private static MacEventHandler INSTANCE;
    
    public static synchronized MacEventHandler instance() {
        if (INSTANCE==null)
            INSTANCE = new MacEventHandler();
        
        return INSTANCE;
    }
    
    private volatile File lastFileOpened = null;
    private volatile boolean enabled;
    private volatile ExternalControl externalControl = null;
    private volatile Initializer initializer = null;
    
    /** Creates a new instance of MacEventHandler */
    private MacEventHandler() {
        
        MRJAdapter.addAboutListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handleAbout();
            }
        });
        
        MRJAdapter.addQuitApplicationListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handleQuit();
            }
        });
        
        MRJAdapter.addOpenDocumentListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                File file = ((ApplicationEvent)evt).getFile();
                handleOpenFile(file);
            }
        });
        
        MRJAdapter.addReopenApplicationListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handleReopen();
            }
        });
    } 
    
    public void enable(ExternalControl externalControl, Initializer initializer) {
        this.externalControl = externalControl;
        this.initializer = initializer;
        this.enabled = true;
        if(lastFileOpened != null)
            runFileOpen(lastFileOpened);
    }
    
    /**
     * Enable preferences.
     */
    public void enablePreferences() {
        MRJAdapter.setPreferencesEnabled(true);
        
        MRJAdapter.addPreferencesListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handlePreferences();
            }
        });
    }
    
    /**
    * This responds to the selection of the about option by displaying the
    * about window to the user.  On OSX, this runs in a new ManagedThread to handle
    * the possibility that event processing can become blocked if launched
    * in the calling thread.
    */
    private void handleAbout() {      
        GUIMediator.showAboutWindow();
    }
    
    /**
    * This method responds to a quit event by closing the application in
    * the whichever method the user has configured (closing after completed
    * file transfers by default).  On OSX, this runs in a new ManagedThread to handle
    * the possibility that event processing can become blocked if launched
    * in the calling thread.
    */
    private void handleQuit() {
        GUIMediator.applyWindowSettings();
        GUIMediator.close(false);
    }
    
    /**
     * This method handles a request to open the specified file.
     */
    private void handleOpenFile(File file) {
        if(!enabled) {
            lastFileOpened = file;
        } else {
            runFileOpen(file);
        }
    }
    
    private void runFileOpen(File file) {
        String filename = file.getPath();
        if (filename.endsWith("limestart")) {
            initializer.setStartup();
        } else if (filename.endsWith("torrent")) {
            if (!GUIMediator.isConstructed() || !GuiCoreMediator.getLifecycleManager().isStarted())
                externalControl.enqueueControlRequest(file.getAbsolutePath());
            else
                GUIMediator.instance().openTorrent(file);
        } else {
            PackagedMediaFileLauncher.launchFile(filename, false);
        }
    }
    
    private void handleReopen() {
        GUIMediator.handleReopen();
    }
    
    private void handlePreferences() {
        GUIMediator.instance().setOptionsVisible(true);
    }
}
