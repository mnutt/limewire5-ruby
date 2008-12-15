package com.limegroup.gnutella.gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import org.limewire.util.SystemUtils;

/**
 * Manages aspects of LimeWire depending on whether or not
 * it is in the foreground, or if the computer has been idle
 * for a while.
 */
class PowerManager implements RefreshListener, WindowListener {
    
    // The maximum idle time before we switch back to full power.
    private static final int MAX_IDLE_TIME = 5 * 60 * 1000; // 5 minutes
    
    /**
     * Whether or not the window is currently active.
     */
    private boolean _active = true;
            
    
    /**
     * Callback for a window activation event.
     *
     * Sets LimeWire to use its full power.
     */
    public void windowActivated(WindowEvent e) {
        _active = true;
        GuiCoreMediator.getApplicationServices().setFullPower(true);
    }
    
    /**
     * Callback for a window deactivation event.
     *
     * If the deactivation was not from a child Java window,
     * sets LimeWire to use less power.
     */
    public void windowDeactivated(WindowEvent e) {
        // If had an opposite, deactivated from another of our windows.
        if(e.getOppositeWindow() != null)
            return;
        
        _active = false;
            
        if(SystemUtils.getIdleTime() > MAX_IDLE_TIME)
            return;
            
        GuiCoreMediator.getApplicationServices().setFullPower(false);
    }
    
    /** Stubbed our WindowListener method. */
    public void windowClosed(WindowEvent e) {}
    /** Stubbed our WindowListener method. */
    public void windowClosing(WindowEvent e) {}
    /** Stubbed our WindowListener method. */
    public void windowDeiconified(WindowEvent e) {}
    /** Stubbed our WindowListener method. */    
    public void windowIconified(WindowEvent e) {}
    /** Stubbed our WindowListener method. */
    public void windowOpened(WindowEvent e) {}
    
    /**
     * Callback for the every-second update.
     *
     * If the user has been idle for a specified amount of time,
     * let LimeWire use its full power.  Otherwise, if LimeWire isn't
     * the foreground application, use less power.
     */
    public void refresh() {
        if(SystemUtils.getIdleTime() > MAX_IDLE_TIME)
            GuiCoreMediator.getApplicationServices().setFullPower(true);
        else if(!_active)
            GuiCoreMediator.getApplicationServices().setFullPower(false);
    } 
}
