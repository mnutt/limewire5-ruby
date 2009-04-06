package org.limewire.ui.swing.search;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.StringTrieSet;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactory;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactoryImpl;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanelFactory;
import org.limewire.ui.swing.search.resultpanel.NameRendererFactory;
import org.limewire.ui.swing.search.resultpanel.NameRendererFactoryImpl;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilderImpl;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidget;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidgetFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultPropertiesFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncatorImpl;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRuleImpl;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.name.Names;

/**
 * Module to configure Guice bindings for the UI search classes.
 */
public class LimeWireUiSearchModule extends AbstractModule {
    
    /**
     * Configures the bindings for the UI search classes.
     */
    @Override
    protected void configure() {
        bind(AutoCompleteDictionary.class).annotatedWith(Names.named("searchHistory")).toInstance(new StringTrieSet(true));
        bind(SearchHandler.class).to(SearchHandlerImpl.class);
        bind(SearchHandler.class).annotatedWith(Names.named("p2p://")).to(P2PLinkSearchHandler.class);
        bind(SearchHandler.class).annotatedWith(Names.named("text")).to(TextSearchHandlerImpl.class);
        bind(SimilarResultsDetectorFactory.class).to(SimilarResultsDetectorFactoryImpl.class);
        
        bind(SearchResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                SearchResultsPanelFactory.class, SearchResultsPanel.class));
        
        bind(ResultsContainerFactory.class).toProvider(
            FactoryProvider.newFactory(
                ResultsContainerFactory.class, ResultsContainer.class));
        
        bind(SortAndFilterPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                SortAndFilterPanelFactory.class, SortAndFilterPanel.class));
        
        bind(BaseResultPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                BaseResultPanelFactory.class, BaseResultPanel.class));
        
        bind(ListViewTableEditorRendererFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ListViewTableEditorRendererFactory.class, ListViewTableEditorRenderer.class));
        
        bind(SearchTabItemsFactory.class).toProvider(
                FactoryProvider.newFactory(
                        SearchTabItemsFactory.class, SearchTabItems.class));
        
        
        bind(RemoteHostActions.class).to(RemoteHostActionsImpl.class);

        bind(new TypeLiteral<PropertiesFactory<VisualSearchResult>>(){}).to(SearchResultPropertiesFactory.class);
        
        bind(NameRendererFactory.class).to(NameRendererFactoryImpl.class);
        
        bind(SearchHeadingDocumentBuilder.class).to(SearchHeadingDocumentBuilderImpl.class);
        
        bind(SearchResultFromWidgetFactory.class).toProvider(
                FactoryProvider.newFactory(
                        SearchResultFromWidgetFactory.class, SearchResultFromWidget.class));
        
        bind(ListViewRowHeightRule.class).to(ListViewRowHeightRuleImpl.class);
        bind(SearchResultTruncator.class).to(SearchResultTruncatorImpl.class);
    }
}