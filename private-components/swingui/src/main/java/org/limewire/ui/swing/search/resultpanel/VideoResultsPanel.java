package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.OpaqueFileSizeRenderer;
import org.limewire.ui.swing.search.resultpanel.classic.OpaqueQualityRenderer;
import org.limewire.ui.swing.search.resultpanel.classic.OpaqueTimeRenderer;
import org.limewire.ui.swing.search.resultpanel.classic.VideoTableFormat;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class VideoResultsPanel extends BaseResultPanel {

    @AssistedInject
    public VideoResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        DownloadListManager downloadListManager,
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        RemoteHostActions remoteHostActions,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory,
        PropertiesFactory<VisualSearchResult> properties,
        ListViewRowHeightRule rowHeightRule, SaveLocationExceptionHandler saveLocationExceptionHandler,
        SearchResultFromWidgetFactory searchResultFromWidget, IconManager iconManager) {
        
        super(listViewEditorRendererFactory, eventList, new VideoTableFormat(), downloadListManager,
            search, searchInfo, preserver, navigator, remoteHostActions, properties, rowHeightRule, 
            saveLocationExceptionHandler, searchResultFromWidget, iconManager);
        
    }
    
    @Override
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        super.setupCellRenderers(tableFormat);
        setCellRenderer(VideoTableFormat.Columns.SIZE.ordinal(), new OpaqueFileSizeRenderer());
        setCellRenderer(VideoTableFormat.Columns.LENGTH.ordinal(), new OpaqueTimeRenderer());
        setCellRenderer(VideoTableFormat.Columns.QUALITY.ordinal(), new OpaqueQualityRenderer());
    }
}