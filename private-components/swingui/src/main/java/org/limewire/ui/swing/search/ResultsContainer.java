package org.limewire.ui.swing.search;

import ca.odell.glazedlists.EventList;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Scrollable;

import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanelFactory;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.limewire.ui.swing.search.resultpanel.OtherResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ProgramResultsPanelFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;

/**
 * This class is a panel that displays search results.
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class ResultsContainer extends JXPanel {

    private BaseResultPanel currentPanel;
    private Map<String, BaseResultPanel> panelMap =
        new HashMap<String, BaseResultPanel>();
    private SearchViewType mode = SearchViewType.forId(SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue());
    
    private final CardLayout cardLayout = new CardLayout();

    /**
     * See LimeWireUISearchModule for binding information.
     */
    @AssistedInject ResultsContainer(
        @Assisted EventList<VisualSearchResult> eventList, 
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        AllResultsPanelFactory allFactory,
        AudioResultsPanelFactory audioFactory,
        VideoResultsPanelFactory videoFactory,
        ImagesResultsPanelFactory imagesFactory,
        DocumentsResultsPanelFactory documentsFactory,
        OtherResultsPanelFactory otherFactory,
        ProgramResultsPanelFactory programFactory) {
        
        panelMap.put(SearchCategory.ALL.name(),
            allFactory.create(eventList, search, searchInfo, preserver));
        panelMap.put(SearchCategory.AUDIO.name(),
            audioFactory.create(filter(Category.AUDIO, eventList), search, searchInfo, preserver));
        panelMap.put(SearchCategory.VIDEO.name(),
            videoFactory.create(filter(Category.VIDEO, eventList), search, searchInfo, preserver));
        panelMap.put(SearchCategory.IMAGE.name(),
            imagesFactory.create(filter(Category.IMAGE, eventList), search, searchInfo, preserver));
        panelMap.put(SearchCategory.DOCUMENT.name(),
            documentsFactory.create(filter(Category.DOCUMENT, eventList), search, searchInfo, preserver));
        panelMap.put(SearchCategory.PROGRAM.name(),
            programFactory.create(filter(Category.PROGRAM, eventList), search, searchInfo, preserver));
        panelMap.put(SearchCategory.OTHER.name(),
            otherFactory.create(filter(Category.OTHER, eventList), search, searchInfo, preserver));
        
        setLayout(cardLayout);
        
        for (Map.Entry<String, BaseResultPanel> entry : panelMap.entrySet()) {
            add(entry.getValue(), entry.getKey());
        }
    }

    public void synchronizeResultCount(SearchCategory key, final Action action) {
        // Adds itself as a listener to the list & keeps the action in sync.
        new SourceCountMaintainer(
            panelMap.get(key.name()).getResultsEventList(), action);
    }
    
    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setViewType(SearchViewType mode) {
        this.mode = mode;
        currentPanel.setViewType(mode);
    }
    
    void showCategory(SearchCategory category) {
        String name = category.name();
        currentPanel = panelMap.get(name); 
        cardLayout.show(this, name);
        currentPanel.setViewType(mode);
    }
    
    private FilterList<VisualSearchResult> filter(
        final Category category, EventList<VisualSearchResult> eventList) {
        return GlazedListsFactory.filterList(
            eventList, new Matcher<VisualSearchResult>() {
            @Override
            public boolean matches(VisualSearchResult item) {
                return item.getCategory() == category;
            }        
        });
    }

    public Component getScrollPaneHeader() {
        return currentPanel.getScrollPaneHeader();
    }

    public Scrollable getScrollable() {
        return currentPanel.getScrollable();
    }
}