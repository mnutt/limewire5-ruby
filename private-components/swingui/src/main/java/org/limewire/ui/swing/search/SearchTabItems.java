package org.limewire.ui.swing.search;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.application.Resource;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.FancyTabListFactory;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * This class contains the numbers of different types of files
 * that matched given search criteria.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
class SearchTabItems {
    
    private final List<TabActionMap> searchActionMaps;

    private final List<SearchTabListener> listeners;
    
    private final FancyTabList searchTab;
    
    @Resource private Font tabFont;

    @AssistedInject
    SearchTabItems(@Assisted SearchCategory category, @Assisted EventList<VisualSearchResult> resultsList, 
            FancyTabListFactory fancyTabListFactory) {
        
        this.listeners = new CopyOnWriteArrayList<SearchTabListener>();
        
        this.searchActionMaps = new ArrayList<TabActionMap>();
        
        GuiUtils.assignResources(this);
        
        switch(category) {
        case ALL:
            addCategory(tr("All"), SearchCategory.ALL);
            addCategory(tr("Audio"), SearchCategory.AUDIO);
            addCategory(tr("Videos"), SearchCategory.VIDEO);
            addCategory(tr("Images"), SearchCategory.IMAGE);
            addCategory(tr("Documents"), SearchCategory.DOCUMENT);
            addCategory(tr("Programs"), SearchCategory.PROGRAM);
            addCategory(tr("Other"), SearchCategory.OTHER);
            break;
        case AUDIO:
            addCategory(tr("Audio results"), SearchCategory.AUDIO);
            break;
        case VIDEO:
            addCategory(tr("Video results"), SearchCategory.VIDEO);
            break;
        case IMAGE:
            addCategory(tr("Image results"), SearchCategory.IMAGE);
            break;
        case DOCUMENT:
            addCategory(tr("Document results"), SearchCategory.DOCUMENT);
            break;
        case PROGRAM:
            addCategory(tr("Program results"), SearchCategory.PROGRAM);
            break;
        default:
            throw new IllegalArgumentException("invalid category: " + category);
        }

        // Select the appropriate one.
        for (TabActionMap map : searchActionMaps) {
            SearchTabAction action = (SearchTabAction) map.getMainAction();
            if (category == action.getCategory()) {
                action.putValue(Action.SELECTED_KEY, true);
            } else if (category != SearchCategory.ALL) {
                action.setEnabled(false);
            }
        }

        searchTab = fancyTabListFactory.create(searchActionMaps);
        searchTab.setTabTextColor(Color.WHITE);
        
        // Make all the tabs except "All" invisible
        // until we get a matching search result.
        if(category == SearchCategory.ALL) {
            searchTab.setTabsVisible(false);
            for(FancyTab tab : searchTab.getTabs()) {
                if(((SearchTabAction)tab.getTabActionMap().getMainAction()).getCategory() == SearchCategory.ALL) {
                    tab.setVisible(true);
                }
            }
        }

        searchTab.setTextFont(tabFont);
        
        // Make sure that we make tabs visible as time goes by.
        if(category == SearchCategory.ALL) {
            resultsList.addListEventListener(new TabMaintainer(searchTab.getTabs()));
        }
    }
    
    void addSearchTabListener(SearchTabListener listener) {
        listeners.add(listener);
        listener.categorySelected(getSelectedCategory());
    }
    
    private void addCategory(String title, SearchCategory category) {
        TabActionMap map = newTabActionMap(new SearchTabAction(title, category));
        searchActionMaps.add(map);
    }
    
    public Collection<Map.Entry<SearchCategory, Action>> getResultCountActions() {
        Map<SearchCategory, Action> counts =
            new EnumMap<SearchCategory, Action>(SearchCategory.class);

        for (TabActionMap map : searchActionMaps) {
            SearchCategory category =((SearchTabAction) map.getMainAction()).getCategory();
            counts.put(category, map.getMoreTextAction());
        }

        return counts.entrySet();
    }
    
    private class TabMaintainer implements ListEventListener<VisualSearchResult> {
        private final EnumMap<SearchCategory, FancyTab> categoryToMap;
        
        public TabMaintainer(List<FancyTab> tabs) {
            categoryToMap = new EnumMap<SearchCategory, FancyTab>(SearchCategory.class);
            // Synchronize the categoryToTab list.
            for(FancyTab tab : tabs) {
                SearchTabAction action = (SearchTabAction)tab.getTabActionMap().getMainAction();
                if(action.getCategory() != SearchCategory.ALL) {
                    categoryToMap.put(action.getCategory(), tab);
                }
            }
        }

        
        @Override
        public void listChanged(ListEvent<VisualSearchResult> listChanges) {
            EventList<VisualSearchResult> source = listChanges.getSourceList();
            if (!listChanges.isReordering()) {                
                while (listChanges.next()) {                    
                    if(listChanges.getType() == ListEvent.INSERT) {
                        VisualSearchResult added = source.get(listChanges.getIndex());
                        SearchCategory searchCategory = SearchCategory.forCategory(added.getCategory());
                        FancyTab tab = categoryToMap.remove(searchCategory);
                        if(tab != null && !tab.isVisible()) {
//                            tab.underline();
                            tab.setVisible(true);
                        }
                    }
                    // If no more categories to scan for, exit & be done with.
                    if(categoryToMap.isEmpty()) {
                        source.removeListEventListener(this);
                        break;
                    }
                }
            }
        }
    }
    
    public FancyTabList getSearchTab() {
        return searchTab;
    }
    
    public SearchCategory getSelectedCategory() {
        return ((SearchTabAction)searchTab.getSelectedTab().getTabActionMap().getMainAction()).getCategory();
    }
    
    private TabActionMap newTabActionMap(SearchTabAction action) {
        Action moreText = new NoOpAction();
        moreText.putValue(Action.NAME, "");
        return new TabActionMap(action, null, moreText, null);
    }

    private class SearchTabAction extends AbstractAction {
        private final SearchCategory category;

        public SearchTabAction(String name, SearchCategory category) {
            super(name);
            this.category = category;
        }

        SearchCategory getCategory() {
            return category;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for(SearchTabListener listener : listeners) {
                listener.categorySelected(category);
            }
        }
    }

    static interface SearchTabListener {
        void categorySelected(SearchCategory searchCategory);
    }
}