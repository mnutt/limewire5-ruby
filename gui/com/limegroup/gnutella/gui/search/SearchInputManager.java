package com.limegroup.gnutella.gui.search;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.core.settings.UISettings;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/**
 * Manages input for the search, including filters for active searches.
 */
final class SearchInputManager implements ThemeObserver {
    /**
     * The panel that contains all input information for searching.
     * This includes both 'input boxes' and 'filter boxes'.
     */
    private final JPanel COMPONENT_PANEL = new JPanel(new GridBagLayout());
    
    /**
     * The card layout switching between searching or filtering.
     */
    private final CardLayout MAIN_CARDS = new CardLayout();
    
    /**
     * The panel containing either search input or filters.
     */
    private final JPanel MAIN_PANEL = new JPanel(MAIN_CARDS);

    /**
     * The search input panel.
     */
    private SearchInputPanel SEARCH;
    
    /**
     * The filter input panel.
     */
    private FilterInputPanel FILTER;

    /**
     * Constructs a new search input manager class, including all displayed
     * elements for search input.
     */
    SearchInputManager() {
        updateTheme();
        ThemeMediator.addThemeObserver(this);
    }
    
    public void updateTheme() {
        SEARCH = new SearchInputPanel(GuiCoreMediator.getNetworkManager(), GuiCoreMediator.getNetworkInstanceUtils());
        FILTER = new FilterInputPanel(new ShowSearchListener(), new AutoSearchListener());

        MAIN_PANEL.removeAll();       
        MAIN_PANEL.add(SEARCH, "search");
        MAIN_PANEL.add(FILTER, "filter");
        
        COMPONENT_PANEL.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(5, 0, 0, 0);
        COMPONENT_PANEL.add(MAIN_PANEL, c);
    }
    
    void rebuild() {
        updateTheme();
    }
    
    void addressChanged() {
        SEARCH.addressChanged();
    }
    
    void goToSearch() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showSearchCard(false);
            }
        });
    }
    
    void requestSearchFocus() {
        requestSearchFocus(false);
    }
    
    /**
     * Returns the <tt>JComponent</tt> instance containing the UI elements
     * for the search input section of the search tab.
     *
     * @return the <tt>JComponent</tt> instance containing the UI elements
     *  for the search input section of the search tab
     */
    JComponent getComponent() {
        return COMPONENT_PANEL;
    }
    
    /**
     * Removes all filters from the list of filters.
     */
    void clearFilters() {
        FILTER.clearFilters();
        showSearchCard(false);
    }
    
    /**
     * Resets the FilterPanel for the specified ResultPanel.
     */
    void panelReset(ResultPanel rp) {
        FILTER.panelReset(rp);
    }
    
    /**
     * Removes the filter associated with the specified result panel.
     */
    void panelRemoved(ResultPanel rp) {
        if(FILTER.panelRemoved(rp))
            showSearchCard(false);
    }
    
    /**
     * Creates and/or displays filters for the specified result panel.
     */
    void setFiltersFor(ResultPanel rp) {
        if(UISettings.SEARCH_RESULT_FILTERS.getValue()) {
            boolean added = FILTER.setFiltersFor(rp);
            MAIN_CARDS.last(MAIN_PANEL);
            if(added)
                requestFilterFocus();
        }
    }

    /**
     * Displays the search card.
     */
    private void showSearchCard(boolean immediate) {
        MAIN_CARDS.first(MAIN_PANEL);
        requestSearchFocus(immediate);
    }      
    
    /**
     * Requests focus for the search field.
     */
    private void requestSearchFocus(boolean immediate) {
        if(immediate)
            SEARCH.requestSearchFocusImmediately();
        else
            SEARCH.requestSearchFocus();
    }
    
    /**
     * Requests focus on the correct area of the filter.
     */
    private void requestFilterFocus() {
        FILTER.requestFilterFocus();
    }    
    
    /**
     * Listener for switching back to the search from a filter.
     */
    private class ShowSearchListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            goToSearch();
        }
    }
    
    /**
     * Listener for key events.
     */
    private class AutoSearchListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            forward(e);
        }
        public void keyReleased(KeyEvent e) {
            forward(e);
        }
        public void keyTyped(KeyEvent e) { 
            if(forward(e)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showSearchCard(true);
                    }
                });
            }
        }
        
        /**
         * Forwards a key event to the search field.
         */
        private boolean forward(KeyEvent e) {
            if(SEARCH.isKeyEventForwardable()) {
                SEARCH.getForwardingSearchField().processKeyEvent(e);
                return true;
            } else {
                return false;
            }
        }
    }
        
}
