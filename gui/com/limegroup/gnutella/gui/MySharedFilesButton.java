package com.limegroup.gnutella.gui;

import java.awt.Dimension;

import com.limegroup.gnutella.gui.actions.MySharedFilesAction;

public final class MySharedFilesButton extends URLLabel {
    
    public MySharedFilesButton() {
        super(new MySharedFilesAction());
        
        setMinimumSize(new Dimension(150, 20));
    }
}
