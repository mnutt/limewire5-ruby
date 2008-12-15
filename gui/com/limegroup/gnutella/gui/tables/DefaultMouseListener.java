package com.limegroup.gnutella.gui.tables;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.SwingUtilities;

/**
 * This class handles mouse input to the component.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class DefaultMouseListener implements MouseListener {

    private MouseObserver cm;
    
    public DefaultMouseListener(MouseObserver mo) {
        this.cm = mo;
    }
    

    /**
     * Invoked when the mouse has been clicked on a component.
	 *
	 * @param event the <tt>MouseEvent</tt> that triggered this call
     */
    public void mouseClicked(MouseEvent event) {
        if (tryPopup(event)) return;
        
		if (SwingUtilities.isRightMouseButton(event)) {
			cm.handleRightMouseClick(event);
		} else if(event.getClickCount() >= 2) {
			cm.handleMouseDoubleClick(event);
		}
	}

    /**
     * Invoked when a mouse button has been pressed on a component.
	 *
	 * @param event the <tt>MouseEvent</tt> that triggered this call
     */
    public void mousePressed(MouseEvent event) { tryPopup(event); }

    /**
     * Invoked when a mouse button has been released on a component.
	 *
	 * @param event the <tt>MouseEvent</tt> that triggered this call
     */
    public void mouseReleased(MouseEvent event) { tryPopup(event); }

    /**
     * Invoked when the mouse enters a component.
	 *
	 * @param event the <tt>MouseEvent</tt> that triggered this call
     */
    public void mouseEntered(MouseEvent event) {}

    /**
     * Invoked when the mouse exits a component.
	 *
	 * @param event the <tt>MouseEvent</tt> that triggered this call
     */
    public void mouseExited(MouseEvent event) {}
    
    public boolean tryPopup(MouseEvent ev) {
        if (ev.isPopupTrigger()) {
            cm.handlePopupMenu(ev);
            return true;
        }
        return false;
    }
                
}
