package com.limegroup.gnutella.gui;

import java.awt.Font;
import java.awt.Insets;

import javax.swing.JEditorPane;
import javax.swing.JLabel;

public class HTMLLabel extends JEditorPane {
    
    public HTMLLabel(String html) {
        super("text/html", html);
        setMargin(new Insets(5, 5, 5, 5));
        setEditable(false);
        setCaretPosition(0);
        addHyperlinkListener(GUIUtils.getHyperlinkListener());
        
        // make it mimic a JLabel
        JLabel label = new JLabel();
        setBackground(label.getBackground());
        setFont(new Font(label.getFont().getName(), 
                         label.getFont().getStyle(),
                         label.getFont().getSize()));
    }

}
