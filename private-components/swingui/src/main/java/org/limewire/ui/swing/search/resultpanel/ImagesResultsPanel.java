package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.ImageTableFormat;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ImagesResultsPanel extends BaseResultPanel {

    @AssistedInject
    public ImagesResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        DownloadListManager downloadListManager,
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory,
        PropertiesFactory<VisualSearchResult> properties,
        ListViewRowHeightRule rowHeightRule, 
        SaveLocationExceptionHandler saveLocationExceptionHandler,
        SearchResultFromWidgetFactory searchResultFromWidget, IconManager iconManager, CategoryIconManager categoryIconManager,
        LibraryNavigator libraryNavigator,
        LibraryManager libraryManager) {
        
        super(listViewEditorRendererFactory,
            eventList, new ImageTableFormat(), downloadListManager,
            search, searchInfo, preserver, navigator, properties, rowHeightRule, 
            saveLocationExceptionHandler, searchResultFromWidget, iconManager, categoryIconManager,
            libraryNavigator, libraryManager);
        
    }
    
    @Override
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        super.setupCellRenderers(tableFormat);
        setCellRenderer(ImageTableFormat.SIZE_INDEX, new FileSizeRenderer());
    }
}