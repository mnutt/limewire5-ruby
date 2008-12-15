package com.limegroup.gnutella.gui.util;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Utilities relating to JPopupMenu & JMenus.
 */
public class PopupUtils {

    /** Adds a menu item defined by the ActionListener & String to the JPopupMenu, enabled or not. */
    public static final void addMenuItem(String s, ActionListener l, JPopupMenu m, boolean enable) {
        addMenuItem(s, l, m, enable, -1);
    }
    /** Adds a menu item defined by the ActionListener & String to the JPopupMenu, enabled or not at the given index. */
    public static final void addMenuItem(String s, ActionListener l, JPopupMenu m, boolean enable, int idx) {
        JMenuItem item = new JMenuItem(s);
        item.addActionListener(l);
        item.setEnabled(enable);
        m.add(item, idx);
    }

    /** Adds a menu item defined by the ActionListener & String to the JMenu, enabled or not. */
    public static final void addMenuItem(String s, ActionListener l, JMenu m, boolean enable) {
        addMenuItem(s, l, m, enable, -1);
    }
    
    /** Adds a menu item defined by the ActionListener & String to the JMenu, enabled or not at the given index. */
    public static final void addMenuItem(String s, ActionListener l, JMenu m, boolean enable, int idx) {
        JMenuItem item = new JMenuItem(s);
        item.addActionListener(l);
        item.setEnabled(enable);
        m.add(item, idx);
    }

}
