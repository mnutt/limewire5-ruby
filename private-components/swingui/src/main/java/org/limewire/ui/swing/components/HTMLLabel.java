package org.limewire.ui.swing.components;

import java.awt.Font;
import java.awt.Insets;

import javax.swing.JEditorPane;
import javax.swing.JLabel;

import org.limewire.ui.swing.util.GuiUtils;

public class HTMLLabel extends JEditorPane {
    
    public HTMLLabel(String html) {
        super("text/html", html);
        setMargin(new Insets(5, 5, 5, 5));
        setEditable(false);
        setCaretPosition(0);
        addHyperlinkListener(GuiUtils.getHyperlinkListener());
        
        // make it mimic a JLabel
        JLabel label = new JLabel();
        setBackground(label.getBackground());
        setFont(new Font(label.getFont().getName(), 
                         label.getFont().getStyle(),
                         label.getFont().getSize()));
    }
}
