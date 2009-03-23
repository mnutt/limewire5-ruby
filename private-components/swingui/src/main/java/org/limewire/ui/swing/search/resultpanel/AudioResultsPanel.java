package org.limewire.ui.swing.search.resultpanel;

import javax.swing.JLabel;

import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.AudioTableFormat;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TimeRenderer;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class AudioResultsPanel extends BaseResultPanel {

    @AssistedInject
    public AudioResultsPanel(
        @Assisted SearchResultsModel searchResultsModel,
        @Assisted EventList<VisualSearchResult> eventList,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory,
        PropertiesFactory<VisualSearchResult> properties,
        ListViewRowHeightRule rowHeightRule, 
        SearchResultFromWidgetFactory searchResultFromWidget,
        LibraryNavigator libraryNavigator,
        NameRendererFactory nameRendererFactory) {
        
        super(searchResultsModel, listViewEditorRendererFactory, eventList, new AudioTableFormat(), 
            preserver, navigator, properties, rowHeightRule,
            searchResultFromWidget,
            libraryNavigator, nameRendererFactory, false);
    }
    
    @Override
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        super.setupCellRenderers(tableFormat);
        setHeaderRenderer(AudioTableFormat.LENGTH_INDEX, new TableCellHeaderRenderer(JLabel.TRAILING));
        setCellRenderer(AudioTableFormat.SIZE_INDEX, new FileSizeRenderer());
        setCellRenderer(AudioTableFormat.LENGTH_INDEX, new TimeRenderer());
        setCellRenderer(AudioTableFormat.QUALITY_INDEX, new QualityRenderer());
    }
}