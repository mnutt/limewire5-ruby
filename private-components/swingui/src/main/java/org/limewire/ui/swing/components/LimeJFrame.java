package org.limewire.ui.swing.components;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JFrame;

import org.limewire.util.SystemUtils;


/**
 * A JFrame that uses LimeWire's icon.
 */
public class LimeJFrame extends JFrame {
    
    private final LimeIconInfo iconInfo = new LimeIconInfo();
    
    public LimeJFrame() throws HeadlessException {
        super();
        initialize();
    }

    public LimeJFrame(GraphicsConfiguration gc) {
        super(gc);
        initialize();
    }

    public LimeJFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        initialize();
    }

    public LimeJFrame(String title) throws HeadlessException {
        super(title);
        initialize();
    }

    private void initialize() {
        setIconImage(iconInfo.getImage());
    }

    // Overrides addNotify() to change to a platform specific icon right afterwards.
    @Override
	public void addNotify() {
		super.addNotify();
		SystemUtils.setWindowIcon(this, iconInfo.getIconFile());
	}
}
