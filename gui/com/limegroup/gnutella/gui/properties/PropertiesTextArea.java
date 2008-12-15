package com.limegroup.gnutella.gui.properties;

import javax.swing.JTextArea;

/** A wrapping, read-only text area that lets the user select and copy. */
public class PropertiesTextArea extends JTextArea {
    
    public PropertiesTextArea(String s) {
        super(s);
        setLineWrap(true);
        setOpaque(false);
        setBorder(null);
        setEditable(false);
    }
}
