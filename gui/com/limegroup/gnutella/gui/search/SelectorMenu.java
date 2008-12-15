package com.limegroup.gnutella.gui.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


/**
 * Manages creating and displaying a JPopupMenu of selectors.
 */
class SelectorMenu {
    
    /**
     * The property which the selector should be put in.
     */
    private static final String SELECTOR = "selector";
    
    /**
     * The actual JPopupMenu.
     */
    private final JPopupMenu MENU = new JPopupMenu();
    
    /**
     * Constructs a new SelectorMenu.
     */
    SelectorMenu(FilterBox box) {
        ActionListener listener = new SelectionListener(box);
        
        List options = box.getMetadataModel().getSelectorOptions();
        String currentSchema = "";
        JMenu currentSchemaMenu = null;
        for(Iterator i = options.iterator(); i.hasNext();) {
            Selector selector = (Selector)i.next();
            JMenuItem item = createItem(box, selector);
            item.addActionListener(listener);

            if(selector.isFieldSelector()) {
                if(!selector.getSchema().equals(currentSchema)) {
                    currentSchema = selector.getSchema();
                    String title = NamedMediaType.getFromDescription(currentSchema).getName();
                    currentSchemaMenu = new JMenu(title);
                    MENU.add(currentSchemaMenu);
                }
                assert currentSchemaMenu != null;
                currentSchemaMenu.add(item);
            } else {
                MENU.add(item);
            }
        }
    }
    
    /**
     * Returns the component to display.
     */
    JPopupMenu getComponent() {
        return MENU;
    }
    
    /**
     * Constructs a new JCheckBoxMenuItem for the specified selector.
     */
    private JMenuItem createItem(FilterBox box, Selector selector) {
        String title = selector.getTitle();
        boolean isSelected = box.getSelector().equals(selector);
        JMenuItem item = new JCheckBoxMenuItem(title, isSelected);
        item.putClientProperty(SELECTOR, selector);
        return item;
    }
    
    /**
     * Constructs a SelectionListener to change the Selector.
     */
    private static class SelectionListener implements ActionListener {
        private final FilterBox BOX;
        
        SelectionListener(FilterBox box) {
            BOX = box;
        }
        
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem)e.getSource();
            Selector selector = (Selector)item.getClientProperty(SELECTOR);
            BOX.setSelector(selector);
        }
    }
}
      