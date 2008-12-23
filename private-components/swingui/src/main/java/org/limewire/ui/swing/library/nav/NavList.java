package org.limewire.ui.swing.library.nav;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

class NavList extends JXPanel {
    
    private final List<NavPanel> navPanels = new ArrayList<NavPanel>();
    
    private final NavPanelMoveAction panelMoveUpAction;
    private final NavPanelMoveAction panelMoveDownAction;
    
    private final JXLabel titleLabel;
    private final JXCollapsiblePane collapsablePanels;
    private final JXPanel panelContainer;
    
    NavList() {
        GuiUtils.assignResources(this);
        
        setLayout(new VerticalLayout(0));
        
        this.panelMoveUpAction = new NavPanelMoveAction(false);
        this.panelMoveDownAction = new NavPanelMoveAction(true);
        
        this.titleLabel = new ActionLabel(new AbstractAction("Nav List Title") {
            @Override
            public void actionPerformed(ActionEvent e) {
                collapsablePanels.setCollapsed(!collapsablePanels.isCollapsed());
            }
        }, false, true);
        FontUtils.bold(titleLabel);
        titleLabel.setName("LibraryNavigator.NavListTitle");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        add(titleLabel);
        
        panelContainer = new JXPanel(new VerticalLayout(0)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = NavList.this.getWidth();
                return d;
            }
        };
        collapsablePanels = new JXCollapsiblePane();
        collapsablePanels.setContentPane(panelContainer);
        add(collapsablePanels);
        
        checkVisibility(true);
    }
    
    /**
     * Checks and sets the visibility for this navlist. 
     * If the nav list is empty the visibility is set to false. 
     * Otherwise is is set to the supplied param canDisplay. 
     */
    protected void checkVisibility(boolean canDisplay) {
        if(navPanels.isEmpty()) {
            setVisible(false);
        } else {
            setVisible(canDisplay);
        }
        invalidate();
        validate();
    }
    
    void setTitleText(String text) {
        titleLabel.setText(text);
    }

    List<NavPanel> clear() {
        List<NavPanel> oldPanels = new ArrayList<NavPanel>(navPanels);
        for(NavPanel panel : oldPanels) {
            removePanel(panel);
        }
        return oldPanels;
    }
    
    List<NavPanel> clearFriends() {
        List<NavPanel> oldPanels = new ArrayList<NavPanel>(navPanels);
        List<NavPanel> removed = new ArrayList<NavPanel>();
        for(NavPanel panel : oldPanels) {
            if(!panel.getFriend().isAnonymous()) {
                removePanel(panel);
                removed.add(panel);
            }
        }
        return removed;
    }
    
    NavPanel ensureFriendVisible(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            collapsablePanels.setCollapsed(false);
            collapsablePanels.scrollRectToVisible(panel.getBounds());
            if(panel == navPanels.get(0)) {
                scrollRectToVisible(titleLabel.getBounds());
            }
        }
        return panel;
        
    }
    
    NavPanel getPanelForFriend(Friend friend) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                return panel;
            }
        }
        return null;
    }    

    void addNavPanel(NavPanel panel) {
        // Find the index where to insert.
        int idx = Collections.binarySearch(navPanels, panel, new Comparator<NavPanel>() {
            @Override
            public int compare(NavPanel o1, NavPanel o2) {
                Friend f1 = o1.getFriend();
                Friend f2 = o2.getFriend();
                if(o1 == o2) {
                    return 0;
                } else if(f1.isAnonymous() && !f2.isAnonymous()) {
                    return 1;
                } else if(f2.isAnonymous() && !f1.isAnonymous()) {
                    return -1;
                } else {           
                    return f1.getRenderName().compareToIgnoreCase(f2.getRenderName());
                }
            }
        });
        int insertIdx = idx >= 0 ? idx : -(idx+1);
        navPanels.add(insertIdx, panel);
        panelContainer.add(panel, insertIdx);

        panel.setParentList(this);
        panel.getActionMap().put(NavKeys.MOVE_DOWN, panelMoveDownAction);
        panel.getActionMap().put(NavKeys.MOVE_UP, panelMoveUpAction);
        
        checkVisibility(true);
    }
       
    private NavPanel moveDown() {
        ListIterator<NavPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            NavPanel panel = iter.next();
            if(panel.hasSelection()) {
                if(iter.hasNext()) {
                    panel = iter.next();
                    panel.select();
                    return panel;
                }
                break;
            }
        }
        return null;
    }
    
    private void moveDownFromThis() {
        // If we couldn't move down, tell the whole list to move
        Action action = NavList.this.getActionMap().get(NavKeys.MOVE_DOWN);
        if(action != null) {
            action.actionPerformed(null);
        }
    }
    
    private void moveUpFromThis() {
        // If we couldn't move up, tell the whole list to move.
        Action action = NavList.this.getActionMap().get(NavKeys.MOVE_UP);
        if(action != null) {
            action.actionPerformed(null);
        }
    }
     
    private NavPanel moveUp() {
        ListIterator<NavPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            NavPanel panel = iter.next();
            if(panel.hasSelection()) {
                iter.previous();
                if(iter.hasPrevious()) {
                    panel = iter.previous();
                    panel.select();
                    return panel;
                }
                break;
            }
        }
        return null;
    }
    
    void removePanel(NavPanel panel) {
        panel.setParentList(null);
        navPanels.remove(panel);
        panelContainer.remove(panel);
        checkVisibility(true);
    }
    
    NavPanel removePanelForFriend(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            removePanel(panel);
        }
        return panel;
    }
    
    NavPanel selectFirst() {
        if(isVisible() && !navPanels.isEmpty()) {
            NavPanel panel = navPanels.get(0);
            panel.select();
            return panel;
        } else {
            moveDownFromThis();
            return null;
        }
    }

    NavPanel selectLast() {
        if(isVisible() && !navPanels.isEmpty()) {
            NavPanel panel = navPanels.get(navPanels.size()-1);
            panel.select();
            return panel;
        } else {
            moveUpFromThis();
            return null;
        }
    }
    
    NavPanel selectFriendLibrary(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            panel.select();
            panel.showLibraryCard();
        }
        return panel;
    } 
    
    NavPanel selectFriendShareList(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            panel.select();
            panel.showSharingCard();
        }
        return panel;
    } 
    
    private class NavPanelMoveAction extends AbstractAction {
        private final boolean moveDown;
        
        NavPanelMoveAction(boolean down) {
            this.moveDown = down;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(moveDown) {
                if(moveDown() == null) {
                    moveDownFromThis();
                }
            } else {
                if(moveUp() == null) {
                    moveUpFromThis();
                }
            }
        }
    }

}
