package org.limewire.ui.swing.search;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.StringTrieSet;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactory;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactoryImpl;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanel;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanel;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.OtherResultsPanel;
import org.limewire.ui.swing.search.resultpanel.OtherResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ProgramResultsPanel;
import org.limewire.ui.swing.search.resultpanel.ProgramResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilderImpl;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidget;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidgetFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultPropertiesFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncatorImpl;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanel;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRuleImpl;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.name.Names;

public class LimeWireUiSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(AutoCompleteDictionary.class).annotatedWith(Names.named("searchHistory")).toInstance(new StringTrieSet(true));
        bind(SearchHandler.class).to(SearchHandlerImpl.class);
        bind(SearchHandler.class).annotatedWith(Names.named("p2p://")).to(P2PLinkSearchHandler.class);
        bind(SearchHandler.class).annotatedWith(Names.named("text")).to(TextSearchHandlerImpl.class);
        bind(RowSelectionPreserver.class).to(RowSelectionPreserverImpl.class);
        bind(SimilarResultsDetectorFactory.class).to(SimilarResultsDetectorFactoryImpl.class);
        
        bind(SearchResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                SearchResultsPanelFactory.class, SearchResultsPanel.class));
        
        bind(ResultsContainerFactory.class).toProvider(
            FactoryProvider.newFactory(
                ResultsContainerFactory.class, ResultsContainer.class));
        
        bind(AllResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                AllResultsPanelFactory.class, AllResultsPanel.class));
        
        bind(AudioResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                AudioResultsPanelFactory.class, AudioResultsPanel.class));
        
        bind(DocumentsResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                DocumentsResultsPanelFactory.class, DocumentsResultsPanel.class));
        
        bind(ImagesResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                ImagesResultsPanelFactory.class, ImagesResultsPanel.class));
        
        bind(OtherResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                OtherResultsPanelFactory.class, OtherResultsPanel.class));
        
        bind(ProgramResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                ProgramResultsPanelFactory.class, ProgramResultsPanel.class));
        
        bind(VideoResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                VideoResultsPanelFactory.class, VideoResultsPanel.class));
        
        bind(ListViewTableEditorRendererFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ListViewTableEditorRendererFactory.class, ListViewTableEditorRenderer.class));
        
        bind(SearchTabItemsFactory.class).toProvider(
                FactoryProvider.newFactory(
                        SearchTabItemsFactory.class, SearchTabItems.class));
        
        
        bind(RemoteHostActions.class).to(RemoteHostActionsImpl.class);

        bind(new TypeLiteral<PropertiesFactory<VisualSearchResult>>(){}).to(SearchResultPropertiesFactory.class);
        
        bind(SearchHeadingDocumentBuilder.class).to(SearchHeadingDocumentBuilderImpl.class);
        
        bind(SearchResultFromWidgetFactory.class).toProvider(
                FactoryProvider.newFactory(
                        SearchResultFromWidgetFactory.class, SearchResultFromWidget.class));
        
        bind(ListViewRowHeightRule.class).to(ListViewRowHeightRuleImpl.class);
        bind(SearchResultTruncator.class).to(SearchResultTruncatorImpl.class);
    }
}