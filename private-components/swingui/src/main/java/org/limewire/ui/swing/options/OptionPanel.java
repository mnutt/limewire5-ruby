package org.limewire.ui.swing.options;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

/**
 * Abstract Option panel for intializing and saving the options within the
 * panel.
 */
public abstract class OptionPanel extends JPanel {

    public OptionPanel() {
    }

    public OptionPanel(String title) {
        setBorder(BorderFactory.createTitledBorder(null, title, 
                TitledBorder.DEFAULT_JUSTIFICATION, 
                TitledBorder.DEFAULT_POSITION, 
                new Font("Dialog", Font.BOLD, 12), new Color(0x313131)));
        setLayout(new MigLayout("nogrid, insets 4, fill"));
        setOpaque(false);
    }

    /**
     * Initializes the options for this panel. Listeners should not be attached
     * in this method. It will be called multiple times as the options dialog is
     * brought up. To prevent memory leaks or recreating the same components
     * this method should only setup the options and rearrange components as
     * necessary. More heavy weight tasks that should only be done once should
     * be done in the constructor.
     */
    public abstract void initOptions();

    abstract boolean applyOptions();

    abstract boolean hasChanged();
}
