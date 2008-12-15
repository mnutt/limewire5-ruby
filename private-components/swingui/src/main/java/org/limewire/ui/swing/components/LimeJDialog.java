package org.limewire.ui.swing.components;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Window;

import javax.swing.JDialog;

import org.limewire.util.SystemUtils;

/**
 * A JDialog that uses LimeWire's icon.
 */
public class LimeJDialog extends JDialog {
    
    private final LimeIconInfo iconInfo = new LimeIconInfo();

    public LimeJDialog() {
        super();
        initialize();
    }

    public LimeJDialog(Dialog owner, boolean modal) {
        super(owner, modal);
        initialize();
    }

    public LimeJDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        initialize();
    }

    public LimeJDialog(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
        initialize();
    }

    public LimeJDialog(Dialog owner, String title) {
        super(owner, title);
        initialize();
    }

    public LimeJDialog(Dialog owner) {
        super(owner);
        initialize();
    }

    public LimeJDialog(Frame owner, boolean modal) {
        super(owner, modal);
        initialize();
    }

    public LimeJDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        initialize();
    }

    public LimeJDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        initialize();
    }

    public LimeJDialog(Frame owner, String title) {
        super(owner, title);
        initialize();
    }

    public LimeJDialog(Frame owner) {
        super(owner);
        initialize();
    }

    public LimeJDialog(Window owner, ModalityType modalityType) {
        super(owner, modalityType);
        initialize();
    }

    public LimeJDialog(Window owner, String title, ModalityType modalityType,
            GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
        initialize();
    }

    public LimeJDialog(Window owner, String title, ModalityType modalityType) {
        super(owner, title, modalityType);
        initialize();
    }

    public LimeJDialog(Window owner, String title) {
        super(owner, title);
        initialize();
    }

    public LimeJDialog(Window owner) {
        super(owner);
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
