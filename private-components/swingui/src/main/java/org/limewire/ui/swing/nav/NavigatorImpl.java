package org.limewire.ui.swing.nav;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import com.google.inject.Singleton;

@Singleton
class NavigatorImpl implements Navigator {
    
    private static final Log LOG = LogFactory.getLog(NavigatorImpl.class);

    // CoW to allow listeners to remove themselves during iteration
    private final List<NavigationListener> listeners = new CopyOnWriteArrayList<NavigationListener>();
    private final List<NavItemImpl> navItems = new ArrayList<NavItemImpl>();
    private final Map<NavCategory, Integer> categoryCount = new EnumMap<NavCategory, Integer>(NavCategory.class);
    private final List<NavItemImpl> selectionHistory = new ArrayList<NavItemImpl>();
    
    private NavItemImpl selectedItem;    
    
    public NavigatorImpl() {
        for(NavCategory category : NavCategory.values()) {
            categoryCount.put(category, 0);
        }
    }
 
    @Override
    public NavItem createNavItem(NavCategory category, String id, JComponent panel) {
        NavItemImpl item = new NavItemImpl(category, id, panel);
        addNavItem(item, panel);
        return item;
    }

    @Override
    public NavItem getNavItem(NavCategory category, String id) {
        for(NavItemImpl item : navItems) {
            LOG.debugf("Returning NavItem for id {0} navItem{1}", id, item);
            if(category.equals(item.category) && id.equals(item.getId())) {
                return item;
            }
        }
        
        return null;
    }

    @Override
    public boolean hasNavItem(NavCategory category, String id) {
        return getNavItem(category, id) != null;
    }
    
    @Override
    public void addNavigationListener(NavigationListener itemListener) {
        listeners.add(itemListener);
        for(NavItemImpl item : navItems) {
            itemListener.itemAdded(item.category, item, item.panel);
        }
    }

    @Override
    public void removeNavigationListener(NavigationListener itemListener) {
        listeners.remove(itemListener);
    }
    
    @Override
    public boolean goBack() {
        if(selectionHistory.size() < 2) {
            return false;
        } else {
            // Remove the current.
            selectionHistory.remove(selectionHistory.size() - 1);
            // Remove & get the prior.
            NavItem item = selectionHistory.remove(selectionHistory.size() - 1);
            // And select it.
            item.select();
            return true;
        }
    }
    
    @Override
    public void showNothing() {
        if(selectedItem != null) {
            NavItemImpl item = selectedItem;
            selectedItem = null;
            item.fireSelected(false);
            for(NavigationListener listener : listeners) {
                listener.itemSelected(null, null, null, null);
            }
        }
    }
        
    private void addNavItem(NavItemImpl item, JComponent panel) {
        LOG.debugf("Adding item {0}", item);
        navItems.add(item);        
        for(NavigationListener listener : listeners) {
            listener.itemAdded(item.category, item, panel);
        }
        
        categoryCount.put(item.category, categoryCount.get(item.category)+1);        
        if(categoryCount.get(item.category) == 1) {
            for(NavigationListener listener : listeners) {
                listener.categoryAdded(item.category);
            }    
        }
    }
    
    /** Removes all instances of item from the history. */
    private void removeFromHistory(NavItemImpl item) {
        for(Iterator<NavItemImpl> iter = selectionHistory.iterator(); iter.hasNext(); ) {
            if(iter.next() == item) {
                iter.remove();
            }
        }
    }
    
    private void addToHistory(NavItemImpl item) {
        selectionHistory.add(item);
        if(selectionHistory.size() > 10) {
            selectionHistory.remove(0);
        }
    }
    
    private void removeNavItem(NavItemImpl item) {
        if(navItems.remove(item)) {
            LOG.debugf("Removed item {0}", item);
            removeFromHistory(item);
            
            for(NavigationListener listener : listeners) {
                listener.itemRemoved(item.category, item, item.panel);
                if(selectedItem == item) {
                    item.fireSelected(false);
                    selectedItem = null;
                    listener.itemSelected(null, null, null, null);
                }
            }
            item.fireRemoved();
            
            categoryCount.put(item.category, categoryCount.get(item.category)-1);
            if(categoryCount.get(item.category) == 0) {
                for(NavigationListener listener : listeners) {
                    listener.categoryRemoved(item.category);
                }
            }
        } else {
            LOG.debugf("Item {0} not contained in list.", item);
        }
    }
    
    private void selectNavItem(NavItemImpl item, NavSelectable selectable) {
        if(item != selectedItem) {
            addToHistory(item);            
            if(selectedItem != null) {
                selectedItem.fireSelected(false);
            }
            selectedItem = item;
            item.fireSelected(true);
            for(NavigationListener listener : listeners) {
                listener.itemSelected(item.category, item, selectable, item.panel);
            }
        }
    }
    
    private class NavItemImpl implements NavItem {
        // CoW to allow listeners to remove themselves during iteration
        private final List<NavItemListener> listeners = new CopyOnWriteArrayList<NavItemListener>();
        private final NavCategory category;
        private final String id;
        private final JComponent panel;
        private boolean valid = true;
        
        public NavItemImpl(NavCategory category, String id, JComponent panel) {
            this.category = category;
            this.id = id;
            this.panel = panel;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public void remove() {
            valid = false;
            removeNavItem(this);
        }
        
        @Override
        public void select() {
            assert valid;
            select(null);
        }
        
        @Override
        public void select(NavSelectable selectable) {
            selectNavItem(this, selectable);
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this);
        }
        
        @Override
        public void addNavItemListener(NavItemListener listener) {
            listeners.add(listener);
        }
        
        @Override
        public void removeNavItemListener(NavItemListener listener) {
            listeners.remove(listener);
        }
        
        void fireSelected(boolean selected) {
            for(NavItemListener listener : listeners) {
                listener.itemSelected(selected);
            }
        }
        
        void fireRemoved() {
            for(NavItemListener listener : listeners) {
                listener.itemRemoved();
            }
        }
        
        @Override
        public boolean isSelected() {
            return selectedItem == this;
        }
    }
}