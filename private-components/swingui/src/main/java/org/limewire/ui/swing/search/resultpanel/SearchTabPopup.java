package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabProperties;
import org.limewire.ui.swing.components.TabActionMap;

/**
 * This class implements the popup menu that is displayed
 * when a search tab is right-clicked.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchTabPopup extends JPopupMenu {

    public SearchTabPopup(final FancyTab tab) {
        FancyTabProperties props = tab.getProperties();
        TabActionMap map = tab.getTabActionMap();
        
        for (Action action : map.getRightClickActions()) {
            add(action);
        }
        
        if (getComponentCount() != 0 && props.isRemovable()) {
            addSeparator();
        }
        
        if (props.isRemovable()) {
            add(new AbstractAction(props.getCloseOneText()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tab.remove();
                }
            });
            add(map.getRemoveOthers());
            addSeparator();
            add(map.getRemoveAll());
        }
    }

    public void show(MouseEvent e) {
        show((Component) e.getSource(), e.getX() + 3, e.getY() + 3);
    }
}