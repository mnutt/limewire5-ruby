package org.limewire.ui.swing.mainframe;

import java.awt.CardLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavComponent;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavSelectable;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MainPanel extends JPanel {
    
    private static final Log LOG = LogFactory.getLog(MainPanel.class);
    
    private final Map<String, JComponent> keyToComponents = new HashMap<String, JComponent>();

    private final CardLayout cardLayout;
    
    @Inject
    public MainPanel(Navigator navigator) {   
        cardLayout = new CardLayout();
        setLayout(cardLayout);

        this.addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {}
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}
            
            @Override
            public void componentResized(ComponentEvent e) {
                MainPanel.this.revalidate();
            }

        });

        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void itemAdded(NavCategory category, NavItem navItem, JComponent panel) {
                LOG.debugf("Added item {0}", navItem);
                keyToComponents.put(asString(navItem), panel);
                add(panel, asString(navItem));
            }

            @Override
            public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel) {
                LOG.debugf("Removed item {0}", navItem);
                remove(keyToComponents.remove(asString(navItem)));
            }
            @Override
            public void itemSelected(NavCategory category, NavItem navItem,
                    NavSelectable selectable, JComponent panel) {
                LOG.debugf("Selected item {0}", navItem);
                if (navItem != null) {
                    cardLayout.show(MainPanel.this, asString(navItem));
                    if (selectable != null && panel instanceof NavComponent) {
                        NavComponent navComponent = (NavComponent) panel;
                        navComponent.select(selectable);
                    }
                }
            }
            
            @Override public void categoryAdded(NavCategory category) {}
            @Override public void categoryRemoved(NavCategory category) {}
        });
    }
    
    private String asString(Object key) {
        return System.identityHashCode(key) + "";
    }

}