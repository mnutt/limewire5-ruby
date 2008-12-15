package com.limegroup.gnutella.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

class ShutdownWindow extends JDialog {
    
    public ShutdownWindow() {
        super(GUIMediator.getAppFrame());
        setResizable(false);
        setTitle(I18n.tr("Shutting down LimeWire..."));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JComponent pane = (JComponent)getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel(I18n.tr("Please wait while LimeWire shuts down..."));
        pane.add(label, c);
        
        JProgressBar bar = new LimeJProgressBar();
        bar.setIndeterminate(true);
        bar.setStringPainted(false);
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.CENTER;
        pane.add(bar, c);
        
        ((JComponent)getContentPane()).setPreferredSize(new Dimension(250, 80));
        pack();
    }
}