package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * A simple wrapper around {@link JOptionPane} methods that
 * forces the parent component to be a visible frame,
 * allowing it to blink in the task bar.
 */
public class FocusJOptionPane {
    
    /**
     * Contains a string representation of the text of all of the currently 
     * visible message dialogs. Presently this is only applicable to dialogs
     * displayed via showMessageDialog.
     */
    private static final List<String> visibleDialogs = new ArrayList<String>();
    
    public static Component createFocusComponent() {
        JFrame frame = new LimeJFrame("LimeWire");
        frame.setUndecorated(true);
        frame.setSize(0, 0);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width / 2, screenSize.height / 2);
        frame.setVisible(true);
        return frame;
     }
    
    /**
     * @see JOptionPane#showMessageDialog(Component, Object, String, int)
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title,
            int messageType) throws HeadlessException {
        String messageString = message.toString();
        // if the message text is already present in a visible message dialog,
        // then we needn't show it again. just ignore it and return.
        //
        // later, when the dialog is closed, we'll remove its text from this
        // list.
        if (visibleDialogs.contains(messageString))
            return;
            
        visibleDialogs.add(messageString);
        
        boolean dispose = false;
        if(parentComponent == null) {
            parentComponent = createFocusComponent();
            dispose = true;
        }
        
        try {
            JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
        } finally {
            visibleDialogs.remove(messageString);
            
            if (dispose)
                ((JFrame)parentComponent).dispose();
        }
    }

    /**
     * @see JOptionPane#showConfirmDialog(Component, Object, String, int)
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType) throws HeadlessException {
        boolean dispose = false;
        if(parentComponent == null) {
            parentComponent = createFocusComponent();
            dispose = true;
        }
        
        try {
            return JOptionPane.showConfirmDialog(parentComponent, message, title, optionType);
        } finally {
            if(dispose)
                ((JFrame)parentComponent).dispose();
        }
        
        
    }

    /**
     * @see JOptionPane#showOptionDialog(Component, Object, String, int, int,
     *      Icon, Object[], Object)
     */
    public static int showOptionDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType, Icon icon, Object[] options, Object initialValue)
            throws HeadlessException {
        boolean dispose = false;
        if(parentComponent == null) {
            parentComponent = createFocusComponent();
            dispose = true;
        }
        
        try {
            return JOptionPane.showOptionDialog(parentComponent, message, title, optionType,
                    messageType, icon, options, initialValue);
        } finally {
            if(dispose)
                ((JFrame)parentComponent).dispose();
        }
    }

}
