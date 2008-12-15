package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Abstract Option panel for intializing and saving the options within
 * the panel.
 */
public abstract class OptionPanel extends JPanel {

    public OptionPanel() {
        
    }
    
    public OptionPanel(String title) {
        setBorder(BorderFactory.createTitledBorder(title));
        setLayout(new MigLayout("gapy 10"));
        setOpaque(false);
    }
    
    public abstract void initOptions();
    
    abstract boolean applyOptions();
    
    abstract boolean hasChanged();
}
