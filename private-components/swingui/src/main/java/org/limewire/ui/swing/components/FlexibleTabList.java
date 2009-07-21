package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/** 
 * A horizontal container for {@link FancyTab FancyTab} objects.  
 * FlexibleTabList adjusts the number of visible tabs depending on the container
 * size, and displays a "more" button when the actual tab count exceeds the 
 * visible count.  New tabs may be added to the container by calling the 
 * {@link #addTabActionMapAt(TabActionMap, int) addTabActionMapAt()} method.  
 * 
 * <p>FlexibleTabList is used to display the search tabs at the top of the main
 * window.</p>
 */
public class FlexibleTabList extends AbstractTabList {
    private static final int MAX_TAB_WIDTH = 205;
    private static final int MIN_TAB_WIDTH = 115;
    private static final int RIGHT_INSET = 3;
    
    @Resource private Icon moreDefaultIcon;
    @Resource private Icon morePressedIcon;
    @Resource private Icon moreRolloverIcon;
    
    private final ComboBoxDecorator comboBoxDecorator;
    private final Action closeOtherAction;
    private final Action closeAllAction;
    
    private int maxVisibleTabs;
    private int vizStartIdx = -1;
    
    /**
     * Constructs a FlexibleTabList with the specified combobox decorator.
     */
    @Inject
    FlexibleTabList(ComboBoxDecorator comboBoxDecorator) {
        this.comboBoxDecorator = comboBoxDecorator;
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0 0 0 3, gap 0, filly, hidemode 2"));  
        setMinimumSize(new Dimension(0, getMinimumSize().height));
        
        closeOtherAction = new CloseOther();
        closeAllAction = new CloseAll();
        
        maxVisibleTabs = Integer.MAX_VALUE;
        
        // Add listener to adjust tab layout when container is resized. 
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Redo tab layout when number of tabs changes.
                if (calculateVisibleTabCount() != maxVisibleTabs) {
                    layoutTabs();
                }
            }
        });
    }

    /** 
     * Adds a new tab using the specified action map at the specified index. 
     */
    public void addTabActionMapAt(TabActionMap actionMap, int i) {
        FancyTab tab = createAndPrepareTab(actionMap);
        addTab(tab, i);
    }

    /**
     * Creates a new tab with the specified action map.  This calls the 
     * superclass method to create the tab, sets the minimum and maximum widths,
     * and adds close actions. 
     */
    @Override
    protected FancyTab createAndPrepareTab(TabActionMap actionMap) {
        // Create tab.
        FancyTab tab = super.createAndPrepareTab(actionMap);
        
        // Set minimum and maximum widths.
        tab.setMinimumSize(new Dimension(MIN_TAB_WIDTH, tab.getMinimumSize().height));
        tab.setMaximumSize(new Dimension(MAX_TAB_WIDTH, tab.getMaximumSize().height));
        
        // Add Close actions.
        actionMap.setRemoveAll(closeAllAction);
        actionMap.setRemoveOthers(closeOtherAction);
        
        return tab;
    }
    
    /** 
     * Updates the layout to display the visible tabs.  This method removes all 
     * visible tabs, and lays them out again. 
     */
    @Override
    protected void layoutTabs() {
        removeAll();
        List<FancyTab> visibleTabs = getPendingVisibleTabs();
        for (FancyTab tab : visibleTabs) {
            add(tab, "growy");
        }
        
        // Add "more" button if some tabs not visible.
        if (visibleTabs.size() < getTabs().size()) {
            FancyTabMoreButton more = new FancyTabMoreButton(getTabs());
            comboBoxDecorator.decorateIconComboBox(more);
            more.setIcon(moreDefaultIcon);
            more.setPressedIcon(morePressedIcon);
            more.setRolloverIcon(moreRolloverIcon);
            more.setSelectedIcon(morePressedIcon);
            add(more, "gapleft 0:" + String.valueOf(MIN_TAB_WIDTH));
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * Returns the tabs that *should* be visible, based on the currently visible
     * tabs, and the currently selected tab.  This keeps state and assumes the
     * tabs it returns will become visible.
     * <p>
     * The goal is to shift the minimum amount of distance possible, while
     * still keeping the selected tab in view.  If there's no selected tab,
     * this bumps everything to the left one.
     */
    private List<FancyTab> getPendingVisibleTabs() {
        // Calculate maximum visible tabs.
        maxVisibleTabs = calculateVisibleTabCount();
        
        // Determine tabs to display.
        List<FancyTab> tabs = getTabs();
        List<FancyTab> vizTabs;
        if (maxVisibleTabs >= tabs.size()) {
            vizStartIdx = 0;
            vizTabs = tabs;
        } else {        
            // Bump the start down from where it previously was
            // if there's now more room to display more tabs,
            // so that we display as many tabs as possible.
            if (tabs.size() - vizStartIdx < maxVisibleTabs) {
                vizStartIdx = tabs.size() - maxVisibleTabs;
            }
            vizTabs = tabs.subList(vizStartIdx, vizStartIdx + maxVisibleTabs);
            
            // If we had a selection, make sure that we shift in the
            // appropriate distance to keep that selection in view.
            FancyTab selectedTab = getSelectedTab();
            if (selectedTab != null && !vizTabs.contains(selectedTab)) {
                int selIdx = tabs.indexOf(selectedTab);
                if (vizStartIdx > selIdx) { // We have to shift left
                    vizStartIdx = selIdx;
                } else { // We have to shift right
                    vizStartIdx = selIdx - maxVisibleTabs + 1;
                }
                vizTabs = tabs.subList(vizStartIdx, vizStartIdx+maxVisibleTabs);
            }
        }
        
        return vizTabs;
    }
    
    /**
     * Calculates the number of visible tabs that will fit in the container.
     * This is based on the current container width and the minimum tab width.
     */
    private int calculateVisibleTabCount() {
        int visibleTabCount;
        
        // Calculate available width and maximum visible tabs.
        int totalWidth = getSize().width;
        int availWidth = Math.max(totalWidth, MIN_TAB_WIDTH);
        visibleTabCount = availWidth / MIN_TAB_WIDTH;
        
        // Adjust maximum tabs including "more" button if necessary.
        if (visibleTabCount < getTabs().size()) {
            int moreWidth = moreDefaultIcon.getIconWidth();
            availWidth = Math.max(totalWidth - moreWidth - RIGHT_INSET, MIN_TAB_WIDTH);
            visibleTabCount = availWidth / MIN_TAB_WIDTH;
        }
        
        return visibleTabCount;
    }

    /**
     * Recreates the tabs in the container using their action maps.
     */
    private void recreateTabs() {
        List<FancyTab> tabs = getTabs();
        List<TabActionMap> actionMaps = new ArrayList<TabActionMap>(tabs.size());
        for (FancyTab tab : tabs) {
            actionMaps.add(tab.getTabActionMap());
        }
        setTabActionMaps(actionMaps);
    }
    
    /**
     * Sets the text for the Close All tabs action.
     */
    public void setCloseAllText(String closeAllText) {
        getTabProperties().setCloseAllText(closeAllText);
        closeAllAction.putValue(Action.NAME, closeAllText);
    }

    /**
     * Sets the text for the Close tab action.
     */
    public void setCloseOneText(String closeOneText) {
        getTabProperties().setCloseOneText(closeOneText);
    }

    /**
     * Sets the text for the Close All Other tabs action.
     */
    public void setCloseOtherText(String closeOtherText) {
        getTabProperties().setCloseOtherText(closeOtherText);
        closeOtherAction.putValue(Action.NAME, closeOtherText);
    }
    
    /** 
     * Sets whether or not the tabs should render a 'remove' icon. 
     */
    public void setRemovable(boolean removable) {
        getTabProperties().setRemovable(removable);
        recreateTabs();
    }
    
    /**
     * Sets the insets for all tabs.
     */
    public void setTabInsets(Insets insets) {
        getTabProperties().setInsets(insets);
        
        revalidate();
    }
    
    /**
     * Action to remove all tabs from the container.
     */
    private class CloseAll extends AbstractAction {
        public CloseAll() {
            super(getTabProperties().getCloseAllText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            while (!getTabs().isEmpty()) {
                getTabs().get(0).remove();
            }
        }
    }
    
    /**
     * Action to remove all tabs except the current one from the container.
     */
    private class CloseOther extends AbstractAction {
        public CloseOther() {
            super(getTabProperties().getCloseOtherText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            while (getTabs().size() > 1) {
                FancyTab tab = getTabs().get(0);
                if (isFrom(tab, (Component) e.getSource())) {
                    tab = getTabs().get(1);
                }
                tab.remove();
            }
        }
        
        private boolean isFrom(JComponent parent, Component child) {
            while (child.getParent() != null) {
                child = child.getParent();
                if (child instanceof JPopupMenu) {
                    child = ((JPopupMenu) child).getInvoker();
                }
                
                if (child == parent) {
                    return true;
                }
            }
            return false;
        }
    }
}
