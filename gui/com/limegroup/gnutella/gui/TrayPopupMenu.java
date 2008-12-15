package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.Window;
import java.lang.reflect.Field;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.SystemUtils;


/**
 * JPopupMenu containing hooks to make sure the class
 * this is contained in when shown is always on top.
 */
public class TrayPopupMenu extends JPopupMenu {
    
    private static final Log LOG = LogFactory.getLog(TrayPopupMenu.class);
    
    /** Whether or not we've disabled the hack, due to failing it once. */
    private boolean hackDisabled = false;
    /** The cached accesable-ized field, after retrieving it once. */
    private Field cachedField;
    
    @Override
    public void setInvoker(Component invoker) {
        Window anc = SwingUtilities.getWindowAncestor(invoker);
        if(anc != null) 
            SystemUtils.setWindowTopMost(anc);
        super.setInvoker(invoker);
    }

    /*
     * Overriden to disable the popup-adjustment for the system tray.
     *  
     * (non-Javadoc)
     * @see javax.swing.JPopupMenu#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean b) {
        boolean doHack = false;
        
        // turn on the hack if we're making it visible.
        if(b && !isVisible())
            doHack = true;
        
        boolean previous = false;
        if(doHack)
            previous = setAdjustmentDisabled(true);
        
        try {
            super.setVisible(b);
        } catch(NullPointerException npe) {
            // We've seen NPE's reported from this, by
            //org.jdesktop.jdic.tray.internal.impl.WinTrayIconService$2.popupMenuWillBecomeInvisible
            // attempting to call a method on its popupParentFrame.
            // No real reason why it should happen, so we try again
            // & ignore if it happens again.
            try {
                super.setVisible(b);
            } catch(NullPointerException ignored) {}
        }
        
        if(doHack)
            setAdjustmentDisabled(previous);
    }
    
    /**
     * Alter the package-private adjustPopupMenuLocation method
     * so that it doesn't alter the system tray menu's location.
     */ 
    private boolean setAdjustmentDisabled(boolean newValue) {
        if(hackDisabled)
            return false;
        
        try {
            if(cachedField == null) {
                cachedField = JPopupMenu.class.getDeclaredField("popupPostionFixDisabled");
                cachedField.setAccessible(true);
            }
            boolean prior = cachedField.getBoolean(null);
            cachedField.setBoolean(null, newValue);
            return prior;
        } catch(Throwable t) {
            LOG.warn("Unable to do adjustment hack - disabling", t);
            hackDisabled = true;
            return false;
        }
    }
    
}
