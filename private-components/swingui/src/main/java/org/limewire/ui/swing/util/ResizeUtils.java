package org.limewire.ui.swing.util;

import java.awt.Component;
import java.awt.Dimension;

/**
 * Common methods for adjusting the size of various components 
 */
public class ResizeUtils {
    
    /**
     * Forces a component to a specific height without changing any of the 
     *  width defaults
     */
    public static void forceHeight(Component comp, int height) {
        comp.setMinimumSize(new Dimension((int)comp.getMinimumSize().getWidth(), height));
        comp.setMaximumSize(new Dimension((int)comp.getMaximumSize().getWidth(), height));
        comp.setPreferredSize(new Dimension((int)comp.getPreferredSize().getWidth(), height));
        comp.setSize(new Dimension((int)comp.getSize().getWidth(), height));
    }

    /**
     * Forces a component to a specific width without changing any of the 
     *  height defaults
     */
    public static void forceWidth(Component comp, int width) {
        comp.setMinimumSize(new Dimension(width, (int)comp.getMinimumSize().getHeight()));
        comp.setMaximumSize(new Dimension(width, (int)comp.getMaximumSize().getHeight()));
        comp.setPreferredSize(new Dimension(width, (int)comp.getPreferredSize().getHeight()));
        comp.setSize(new Dimension(width, (int)comp.getSize().getHeight()));
    }
    
    public static void forceSize(Component comp, Dimension d) {
        comp.setMaximumSize(d);
        comp.setMaximumSize(d);
        comp.setPreferredSize(d);
        comp.setSize(d);
    }
}
