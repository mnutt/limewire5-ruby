package com.limegroup.gnutella.gui;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.limewire.util.SystemUtils;


/**
 * A JFrame that uses LimeWire's icon.
 */
public class LimeJFrame extends JFrame {

    
	public LimeJFrame() throws HeadlessException {
        super();
        initialize();
    }

    public LimeJFrame(GraphicsConfiguration arg0) {
        super(arg0);
        initialize();
    }

    public LimeJFrame(String arg0, GraphicsConfiguration arg1) {
        super(arg0, arg1);
        initialize();
    }

    public LimeJFrame(String arg0) throws HeadlessException {
        super(arg0);
        initialize();
    }
    
    private void initialize() {
        ImageIcon limeIcon = GUIMediator.getThemeImage(GUIConstants.LIMEWIRE_ICON);
        setIconImage(limeIcon.getImage());
    }

    // Overrides addNotify() to change to a platform specific icon right afterwards.
    @Override
	public void addNotify() {
		super.addNotify();

		// Replace the Swing icon with a prettier platform-specific one
		SystemUtils.setWindowIcon(this, GUIConstants.LIMEWIRE_EXE_FILE);
	}
}
