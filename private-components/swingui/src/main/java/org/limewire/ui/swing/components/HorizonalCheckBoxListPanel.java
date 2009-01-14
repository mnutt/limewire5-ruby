package org.limewire.ui.swing.components;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

/**
 * A panel that manages a list of check boxes based on a set of keys
 */
public class HorizonalCheckBoxListPanel<K> extends JPanel {
    
    private final Map<K,JCheckBox> optionsMap;
    
    public HorizonalCheckBoxListPanel(Collection<K> options) {
        this(options, null);
    }
    
    /**
     * Creates the panel and selects the set of boxes
     */
    public HorizonalCheckBoxListPanel(Collection<K> options, Collection<K> selected) {
        
        setLayout(new MigLayout("gapx 18"));
        setOpaque(false);
        
        optionsMap = new HashMap<K,JCheckBox>();
        
        for ( K key : options ) {
            JCheckBox box = new JCheckBox(key.toString());
            box.setOpaque(false);
            box.setHorizontalTextPosition(SwingConstants.RIGHT);
            box.setSelected(selected != null && selected.contains(key));
            
            add(box);
            optionsMap.put(key, box);
        }
    }
    
    /**
     * Gets a check box corresponding to a certain key
     */
    public JCheckBox getCheckBox(K key) {
        return optionsMap.get(key);
    }
    
    /**
     * Return keys of all the selected check boxes
     */
    public Collection<K> getSelected() {
        Set<K> selected = new HashSet<K>();
        
        for ( K item : optionsMap.keySet() ) {
            if (optionsMap.get(item).isSelected()) {
                selected.add(item);
            }
        }
        
        return selected;
    }
    
    /**
     * Select the check boxes with the following keys, leave the rest
     */
    public void setSelected(Collection<K> selectedKeys) {
        for ( K key : selectedKeys ) {
            optionsMap.get(key).setSelected(true);
        }
    }
    
}
