package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.LayoutManager;

import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

class LibrarySelectionPanel extends JPanel {
    
    @Resource private Color backgroundColor;

    public LibrarySelectionPanel() {
        super();
        init();
    }

    public LibrarySelectionPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        init();
    }

    public LibrarySelectionPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        init();
    }

    public LibrarySelectionPanel(LayoutManager layout) {
        super(layout);
        init();
    }
    
    private void init() {
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
    }
    
    

}
