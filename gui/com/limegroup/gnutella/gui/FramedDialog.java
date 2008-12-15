package com.limegroup.gnutella.gui;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JDialog;

public class FramedDialog extends LimeJFrame {

    private final JDialog dialog = new JDialog(this);
    
    public FramedDialog() throws HeadlessException {
        super();
        initialize();
    }

    public FramedDialog(GraphicsConfiguration arg0) {
        super(arg0);
        initialize();
    }

    public FramedDialog(String arg0, GraphicsConfiguration arg1) {
        super(arg0, arg1);
        initialize();
    }

    public FramedDialog(String arg0) throws HeadlessException {
        super(arg0);
        initialize();
    }
    
    private void initialize() {
        setUndecorated(true);
        setSize(0, 0);
    }
    
    public void showDialog() {
        toFront();
        setVisible(true);
        dialog.toFront();
        dialog.setVisible(true);
        dispose();
    }
    
    public JDialog getDialog() {
        return dialog;
    }

    public void disposeDialog() {
        dialog.dispose();
    }
    
    
}
