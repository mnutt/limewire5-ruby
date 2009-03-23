package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.ProgramTableFormat;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;
import org.limewire.ui.swing.table.FileSizeRenderer;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ProgramResultsPanel extends BaseResultPanel {

    @AssistedInject
    public ProgramResultsPanel(
        @Assisted SearchResultsModel searchResultsModel,
        @Assisted EventList<VisualSearchResult> eventList,
        ProgramTableFormat tableFormat,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory,
        PropertiesFactory<VisualSearchResult> properties,
        ListViewRowHeightRule rowHeightRule, 
        SearchResultFromWidgetFactory searchResultFromWidget,
        LibraryNavigator libraryNavigator,
        NameRendererFactory nameRendererFactory) {
            
        super(searchResultsModel, listViewEditorRendererFactory, eventList, tableFormat, 
            preserver, navigator, properties, rowHeightRule, 
            searchResultFromWidget,
            libraryNavigator, nameRendererFactory, false);
    }
    
    @Override
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        super.setupCellRenderers(tableFormat);
        setCellRenderer(ProgramTableFormat.SIZE_INDEX, new FileSizeRenderer());
    }
}