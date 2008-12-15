package com.limegroup.gnutella.gui.properties;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.limegroup.gnutella.gui.I18n;

/** A JPanel that shows key and value pairs in rows. */
public class PropertiesPanel extends JPanel {
    
    /** The row number we're on, counts up from 0 as we add rows. */
    private int row;
    
    /** Constraints we use to add a new row. */
    private GridBagConstraints c;
    
    /** Margin space used in layout. */
    public static final int space = 6;
    
    /** Make a panel that shows key and value pairs. */
    public PropertiesPanel() {
        setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(space, space, space, space);
    }
    
    /** Adds all the keys and values in map to this panel. */
    public void add(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet())
            add(entry.getKey(), entry.getValue());
    }
    
    /** Adds the given key and its value in p to a new row in this panel. */
    public void add(String key, String value) {
        JComponent left = new JLabel(I18n.tr(key));
        JComponent right = new PropertiesTextArea(value);
        add(left, right);
    }

    /** Adds the given left and right components in a new row in this panel. */
    public void add(JComponent left, JComponent right) {
        
        // Set this row and move to the next one for next time
        c.gridy = row++;

        // Add left
        c.gridx = 0;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(left, c);
        
        // Add right
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        add(right, c);
    }
}
