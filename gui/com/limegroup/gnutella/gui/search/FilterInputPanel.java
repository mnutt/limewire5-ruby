package com.limegroup.gnutella.gui.search;

import java.awt.CardLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconButton;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeSettings;

class FilterInputPanel extends BoxPanel {

    /**
     * The ditherer drawing the back to search button.
     */
    private final Ditherer DITHERER = 
            new Ditherer(20,
                        ThemeFileHandler.SEARCH_PANEL_BG_1.getValue(), 
                        ThemeFileHandler.SEARCH_PANEL_BG_2.getValue()
                        );
        
    /**
     * The CardLayout that switches among the various FilterPanels
     * or the search input panel.
     */
    private final CardLayout FILTER_CARDS = new CardLayout();
    
    /**
     * The panel used by FILTER_CARDS that displays either filter boxes
     * or detailed search information.
     */
    private final JPanel FILTER_PANEL = new JPanel(FILTER_CARDS);
    
    /**
     * A Mapping from ResultPanel to FilterPanel, to keep track of which
     * filters are active for results.
     */
    private final Map<ResultPanel, FilterPanel> ACTIVE_FILTERS =
        new HashMap<ResultPanel, FilterPanel>();
    
    /**
     * The 'back to search button', for focus requesting.
     */
    private JButton backToSearch;
    
    FilterInputPanel(ActionListener forButton, KeyListener autoSearch) {
        super(BoxPanel.Y_AXIS);
        
        add(Box.createVerticalStrut(4));
        add(GUIUtils.left(new JLabel(
            I18n.tr("Filter Results:"))));
        add(FILTER_PANEL);
        add(Box.createVerticalStrut(4));
        add(createBackToSearchButton(forButton, autoSearch));
    }
    
    /**
     * Requests focus on the search button.
     */
    void requestFilterFocus() {
       SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                backToSearch.requestFocus();
            }
       });
    }
    
    /**
     * Removes all filters from the list of filters.
     */
    void clearFilters() {
        for(Iterator i = ACTIVE_FILTERS.values().iterator(); i.hasNext();) {
            FILTER_PANEL.remove((FilterPanel)i.next());
            i.remove();
        }
        FilterBox.clearRenderer();        
    }
    
    /**
     * Resets the FilterPanel for the specified ResultPanel.
     */
    void panelReset(ResultPanel rp) {
        FilterPanel panel = ACTIVE_FILTERS.get(rp);
        if(panel != null) {
            FILTER_PANEL.remove(panel);
            ACTIVE_FILTERS.remove(rp);
            setFiltersFor(rp);
        }        
    }
    
    /**
     * Removes the filter associated with the specified result panel.
     */
    boolean panelRemoved(ResultPanel rp) {
        FilterPanel panel = ACTIVE_FILTERS.get(rp);
        if(panel != null) {
            FILTER_PANEL.remove(panel);
            ACTIVE_FILTERS.remove(rp);
        }
        FilterBox.clearRenderer();
        return ACTIVE_FILTERS.isEmpty();
    }
    
    /**
     * Creates and/or displays filters for the specified result panel.
     */
    boolean setFiltersFor(ResultPanel rp) {
        boolean added = false;
        FilterPanel panel = ACTIVE_FILTERS.get(rp);
        if(panel == null) {
            added = true;
            panel = new FilterPanel(rp);
			FilterPopupMenuHandler.install(panel);
            FILTER_PANEL.add(panel, panel.getUniqueDescription());
            ACTIVE_FILTERS.put(rp, panel);
        }
        FILTER_CARDS.show(FILTER_PANEL, panel.getUniqueDescription());
        return added;
    }
    
    private JPanel createBackToSearchButton(ActionListener forButton,
                                            KeyListener autoSearch) {
        JButton search = new IconButton(
            I18n.tr("Back To Search"),
            I18nMarker.marktr("Back To Search"));
        search.addActionListener(forButton);
        search.addKeyListener(autoSearch);
        search.setRequestFocusEnabled(true);
    
        JPanel panel = new DitherPanel(DITHERER);
        panel.setLayout(new BoxLayout(panel, BoxPanel.Y_AXIS));
        panel.add(Box.createVerticalStrut(5));
        panel.add(GUIUtils.center(search));
        panel.add(Box.createVerticalStrut(5));
        panel.setBackground(ThemeFileHandler.SEARCH_PANEL_BG_2.getValue());
        GUIUtils.setOpaque(false, panel);
        if(!ThemeSettings.isNativeTheme())
            panel.setOpaque(true);
        panel.setBorder(BorderFactory.createLineBorder(
            ThemeFileHandler.SEARCH_GRID_COLOR.getValue()));
        
        backToSearch = search;
        return panel;
    }    
    
}
