package org.limewire.ui.swing.search.resultpanel;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.Search;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.DefaultLibraryRenderer;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.DownloadItemPropertyListener;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.SearchViewType;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.ClassicDoubleClickHandler;
import org.limewire.ui.swing.search.resultpanel.classic.FromTableCellRenderer;
import org.limewire.ui.swing.search.resultpanel.list.ListViewDisplayedRowsLimit;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableFormat;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.ConfigurableTable;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.VisibleTableFormat;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.EventListJXTableSorting;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.RangeList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventTableModel;

public abstract class BaseResultPanel extends JXPanel implements DownloadHandler {
    
    private static final int MAX_DISPLAYED_RESULT_SIZE = 500;
    private final ListViewTableEditorRendererFactory listViewTableEditorRendererFactory;
    private final Log LOG = LogFactory.getLog(BaseResultPanel.class);
    
    private static final int TABLE_ROW_HEIGHT = 23;
    private static final int ROW_HEIGHT = 56;
    
    private final CardLayout layout = new CardLayout();
    private final EventList<VisualSearchResult> baseEventList;
    private ListViewTable resultsList;
    private ConfigurableTable<VisualSearchResult> resultsTable;
    private final Search search;
    private final DownloadListManager downloadListManager;
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    //cache for RowDisplayResult which could be expensive to generate with large search result sets
    private final Map<VisualSearchResult, RowDisplayResult> vsrToRowDisplayResultMap = 
        new HashMap<VisualSearchResult, RowDisplayResult>();
    
    private Scrollable visibileComponent;
    
    private final SearchResultFromWidgetFactory factory;
    private IconManager iconManager;
    private CategoryIconManager categoryIconManager;
    private List<DownloadPreprocessor> downloadPreprocessors = new ArrayList<DownloadPreprocessor>();
    
    private final LibraryNavigator libraryNavigator;
    private final LibraryManager libraryManager;

    BaseResultPanel(ListViewTableEditorRendererFactory listViewTableEditorRendererFactory,
            EventList<VisualSearchResult> eventList,
            ResultsTableFormat<VisualSearchResult> tableFormat,
            DownloadListManager downloadListManager,
            Search search,
            SearchInfo searchInfo, 
            RowSelectionPreserver preserver,
            Navigator navigator, PropertiesFactory<VisualSearchResult> properties, 
            ListViewRowHeightRule rowHeightRule,
            SaveLocationExceptionHandler saveLocationExceptionHandler,
            SearchResultFromWidgetFactory fromWidgetFactory, IconManager iconManager, CategoryIconManager categoryIconManager,
            LibraryNavigator libraryNavigator,
            LibraryManager libraryManager) {
        
        this.listViewTableEditorRendererFactory = listViewTableEditorRendererFactory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.baseEventList = eventList;
        this.downloadListManager = downloadListManager;
        this.search = search;
        this.factory = fromWidgetFactory;
        this.iconManager = iconManager;
        this.categoryIconManager = categoryIconManager;
        this.downloadPreprocessors.add(new LicenseWarningDownloadPreprocessor());
        this.libraryNavigator = libraryNavigator;
        this.libraryManager = libraryManager;
        
        setLayout(layout);
                
        configureList(eventList, preserver, navigator, searchInfo, properties, rowHeightRule);
        configureTable(eventList, tableFormat, navigator, properties);
 
        add(resultsList, SearchViewType.LIST.name());
        add(resultsTable, SearchViewType.TABLE.name());
        setViewType(SearchViewType.LIST);
    }
    
    private void configureList(final EventList<VisualSearchResult> eventList, RowSelectionPreserver preserver, final Navigator navigator, 
            final SearchInfo searchInfo, final PropertiesFactory<VisualSearchResult> properties, 
            final ListViewRowHeightRule rowHeightRule) {
        
        ListViewTableFormat tableFormat = new ListViewTableFormat();        
        final RangeList<VisualSearchResult> maxSizedList = new RangeList<VisualSearchResult>(newVisibleFilterList(eventList));
        maxSizedList.setHeadRange(0, MAX_DISPLAYED_RESULT_SIZE + 1);
        
        resultsList = new ListViewTable(maxSizedList, tableFormat);
        resultsList.setShowGrid(true, false);
        preserver.addRowPreservationListener(resultsList);
        
        
        // Represents display limits for displaying search results in list view.
        // The limits are introduced to avoid a performance penalty caused by
        // very large (> 1k) search results. Variable row-height in the list
        // view is calculated by looping through all results in the table
        // and if the table holds many results, the performance penalty of 
        // resizing all rows is noticeable past a certain number of rows.
        ListViewDisplayedRowsLimit displayLimit = new ListViewDisplayedRowsLimit() {
            @Override
            public int getLastDisplayedRow() {
                return MAX_DISPLAYED_RESULT_SIZE;
            }

            @Override
            public int getTotalResultsReturned() {
                return eventList.size();
            }
        };

        // Note that the same ListViewTableCellEditor instance
        // cannot be used for both the editor and the renderer
        // because the renderer receives paint requests for some cells
        // while another cell is being edited
        // and they can't share state (the list of sources).
        // The two ListViewTableCellEditor instances
        // can share the same ActionColumnTableCellEditor though.
        ListViewTableEditorRenderer renderer = listViewTableEditorRendererFactory.create(
           new ActionColumnTableCellEditor(this), searchInfo.getQuery(), 
                    navigator, resultsList.getTableColors().selectionColor, this, 
                    displayLimit);
        
        TableColumnModel tcm = resultsList.getColumnModel();
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setCellRenderer(renderer);
        }

        ListViewTableEditorRenderer editor = listViewTableEditorRendererFactory.create(
                new ActionColumnTableCellEditor(this), searchInfo.getQuery(), 
                    navigator, resultsList.getTableColors().selectionColor, this,
                    displayLimit);
        
        resultsList.setDefaultEditor(VisualSearchResult.class, editor);

        // Set default width of all visible columns.
        for (int i = 0; i < tableFormat.getColumnCount(); i++) {
            resultsList.getColumnModel().getColumn(i).setPreferredWidth(tableFormat.getInitialWidth(i));
        }
        
        resultsList.setRowHeightEnabled(true);
        //add listener to table model to set row heights based on contents of the search results
        maxSizedList.addListEventListener(new ListEventListener<VisualSearchResult>() {
            @Override
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                
                final EventTableModel model = (EventTableModel) resultsList.getModel();
                if (model.getRowCount() == 0) {
                    return;
                }
                
                //Push row resizing to the end of the event dispatch queue
                Runnable runner = new Runnable() {
                    @Override
                    public void run() {
                        
                        resultsList.setIgnoreRepaints(true);
                        boolean setRowSize = false;
                        for(int row = 0; row < model.getRowCount(); row++) {
                            VisualSearchResult vsr = (VisualSearchResult) model.getElementAt(row);
                            RowDisplayResult result = vsrToRowDisplayResultMap.get(vsr);
                            if (result == null || result.isStale(vsr)) {
                                result = rowHeightRule.getDisplayResult(vsr, searchInfo.getQuery());
                                vsrToRowDisplayResultMap.put(vsr, result);
                            } 
                            int newRowHeight = result.getConfig().getRowHeight();
                            if (resultsList.getRowHeight(row) != newRowHeight) {
                                LOG.debugf("Row: {0} vsr: {1} config: {2}", row, vsr.getHeading(), 
                                        result.getConfig());
                                resultsList.setRowHeight(row, newRowHeight);
                                setRowSize = true;
                            }
                        }
                        resultsList.setIgnoreRepaints(false);
                        if (setRowSize) {
                            if (resultsList.isEditing()) {
                                resultsList.editingCanceled(new ChangeEvent(resultsList));
                            }
                            resultsList.updateViewSizeSequence();
                            resultsList.resizeAndRepaint();
                        }
                    }
                };
                
                SwingUtilities.invokeLater(runner);
            }
        });
        resultsList.setRowHeight(ROW_HEIGHT);
        
        resultsList.addMouseListener(new ResultDownloaderAdaptor());
    }
    
    private EventList<VisualSearchResult> newVisibleFilterList(
            EventList<VisualSearchResult> eventList) {
        return GlazedListsFactory.filterList(eventList, new Matcher<VisualSearchResult>() {
            @Override
            public boolean matches(VisualSearchResult item) {
                boolean visible = item.isVisible();
                LOG.debugf("filter... VSR urn {0} visibility {1}", item.getCoreSearchResults().get(0).getUrn(), visible);
                return visible;
            }
        });
    }

    private void configureTable(EventList<VisualSearchResult> eventList,
        final ResultsTableFormat<VisualSearchResult> tableFormat, Navigator navigator,
        PropertiesFactory<VisualSearchResult> properties) {
        
        SortedList<VisualSearchResult> sortedList = new SortedList<VisualSearchResult>(eventList);
        resultsTable = new ConfigurableTable<VisualSearchResult>(sortedList, tableFormat, true);

        //link the jxtable column headers to the sorted list
        EventListJXTableSorting.install(resultsTable, sortedList, tableFormat);
            
        setupCellRenderers(tableFormat);
  
        resultsTable.setPopupHandler(new SearchPopupHandler(resultsTable, this, properties));
        resultsTable.setDoubleClickHandler(new ClassicDoubleClickHandler(resultsTable, this, navigator, libraryNavigator));

        resultsTable.setRowHeight(TABLE_ROW_HEIGHT);
        
        resultsTable.setupColumnHandler();

        TableColors tableColors = new TableColors();
        resultsTable.addHighlighter(new ColorHighlighter(new DownloadedHighlightPredicate(sortedList), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
    }

    protected void setupCellRenderers(final ResultsTableFormat<VisualSearchResult> tableFormat) {
        CalendarRenderer calendarRenderer = new CalendarRenderer();
        IconLabelRenderer iconLabelRenderer = new IconLabelRenderer(iconManager, categoryIconManager, downloadListManager, libraryManager);
        TableCellRenderer defaultRenderer = new DefaultLibraryRenderer();
        
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Class clazz = tableFormat.getColumnClass(i);
            if (clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class) {
                setCellRenderer(i, defaultRenderer);
            } else if (clazz == Calendar.class) {
                setCellRenderer(i, calendarRenderer);
            } else if (i == tableFormat.getNameColumn()) {
                setCellRenderer(i, iconLabelRenderer);
            } else if (VisualSearchResult.class.isAssignableFrom(clazz)) {
                setCellRenderer(i, new FromTableCellRenderer(factory.create(true)));
                setCellEditor(i, new FromTableCellRenderer(factory.create(true)));
            }
        }
    }

    protected void setCellRenderer(int column, TableCellRenderer cellRenderer) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellRenderer(cellRenderer);
    }
    
    protected void setCellEditor(int column, TableCellEditor editor) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellEditor(editor);
    }

    public void download(final VisualSearchResult vsr) {
        try {
            // execute the download preprocessors
            for (DownloadPreprocessor preprocessor : downloadPreprocessors) {
                boolean shouldDownload = preprocessor.execute(vsr);
                if (!shouldDownload) {
                    // do not download!
                    return;
                }
            }
            
            DownloadItem di = downloadListManager.addDownload(
                search, vsr.getCoreSearchResults());
            di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
             
            vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
        } catch (final SaveLocationException sle) {
            if(sle.getErrorCode()  == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
                DownloadItem downloadItem = downloadListManager.getDownloadItem(vsr.getUrn());
                if(downloadItem != null) {
                    downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                    vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                }
            } else {
                saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws SaveLocationException {
                        DownloadItem di = downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, overwrite);
                        di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                        vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    }
                }, sle, true);
            }
        }
    }
    
    /**
	 * Paints the foreground of a table row. 
	 */
    private static class DownloadedHighlightPredicate implements HighlightPredicate {
        private SortedList<VisualSearchResult> sortedList;
        public DownloadedHighlightPredicate (SortedList<VisualSearchResult> sortedList) {
            this.sortedList = sortedList;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            VisualSearchResult result = sortedList.get(adapter.row);
            return result.isSpam();
        }       
    }
    
    public EventList<VisualSearchResult> getResultsEventList() {
        return baseEventList;
    }

    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setViewType(SearchViewType mode) {
        layout.show(this, mode.name());
        switch(mode) {
        case LIST: this.visibileComponent = resultsList; break;
        case TABLE: this.visibileComponent = resultsTable; break;
        default: throw new IllegalStateException("unsupported mode: " + mode);
        }
    }

    private class ResultDownloaderAdaptor extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                int row = resultsList.rowAtPoint(e.getPoint());
                if (row == -1 || row == MAX_DISPLAYED_RESULT_SIZE) return;
                TableModel tm = resultsList.getModel();
                VisualSearchResult vsr =
                    (VisualSearchResult) tm.getValueAt(row, 0);
                download(vsr);
            }
        }
    }

    public Component getScrollPaneHeader() {
        return visibileComponent == resultsTable ?
            resultsTable.getTableHeader() : null;
    }

    public Scrollable getScrollable() {
        return visibileComponent;
    }
    
    public static class ListViewTable extends ConfigurableTable<VisualSearchResult> {
        private boolean ignoreRepaints;
        
        public ListViewTable(EventList<VisualSearchResult> eventList, VisibleTableFormat<VisualSearchResult> tableFormat) {
            super(eventList, tableFormat, false);
            
            setGridColor(Color.decode("#EBEBEB"));
        }

        @Override
        protected TableColors newTableColors() {
            TableColors colors = super.newTableColors();
            
            colors.evenColor = Color.WHITE;
            colors.oddColor = Color.WHITE;
            colors.getEvenHighlighter().setBackground(colors.evenColor);
            colors.getOddHighlighter().setBackground(colors.oddColor);
            return colors;
        }
        
        private void setIgnoreRepaints(boolean ignore) {
            this.ignoreRepaints = ignore;
        }
        
        @Override
        protected void updateViewSizeSequence() {
            if (ignoreRepaints) {
                return;
            }
            super.updateViewSizeSequence();
        }

        @Override
        protected void resizeAndRepaint() {
            if (ignoreRepaints) {
                return;
            }
            super.resizeAndRepaint();
        }
    }
}