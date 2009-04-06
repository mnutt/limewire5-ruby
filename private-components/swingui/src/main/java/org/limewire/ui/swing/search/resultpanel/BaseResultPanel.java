package org.limewire.ui.swing.search.resultpanel;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.DefaultLibraryRenderer;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.SearchViewType;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.AllTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.AudioTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.ClassicDoubleClickHandler;
import org.limewire.ui.swing.search.resultpanel.classic.DocumentTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.FromTableCellRenderer;
import org.limewire.ui.swing.search.resultpanel.classic.ImageTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.OtherTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.ProgramTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.VideoTableFormat;
import org.limewire.ui.swing.search.resultpanel.list.ListViewDisplayedRowsLimit;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRendererFactory;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableFormat;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.EventListJXTableSorting;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.RangeList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Base class containing the search results tables for a single category.  
 * BaseResultPanel contains both the List view and Table view components.  The
 * current view is selected by calling the <code>setViewType()</code> method.
 * the display category is selected by calling the <code>showCategory()</code>
 * method.
 */
public class BaseResultPanel extends JXPanel {
    
    private static final int MAX_DISPLAYED_RESULT_SIZE = 500;
    private static final int TABLE_ROW_HEIGHT = 23;
    private static final int ROW_HEIGHT = ListViewRowHeightRule.RowDisplayConfig.HeadingAndMetadata.getRowHeight();

    private final ListViewTableEditorRendererFactory listViewTableEditorRendererFactory;
    private final Log LOG = LogFactory.getLog(BaseResultPanel.class);
    
    private final CardLayout layout = new CardLayout();

    /** Table component for the List view. */
    private final ListViewTable resultsList;

    /** Table component for the Table view. */
    private final ResultsTable<VisualSearchResult> resultsTable;
    
    /** cache for RowDisplayResult which could be expensive to generate with large search result sets */
    private final Map<VisualSearchResult, RowDisplayResult> vsrToRowDisplayResultMap = 
        new HashMap<VisualSearchResult, RowDisplayResult>();
    
    /** Data model containing search results. */
    private final SearchResultsModel searchResultsModel;
    
    private final ResultsTableFormatFactory tableFormatFactory;
    private final Navigator navigator;
    private final PropertiesFactory<VisualSearchResult> properties;
    private final ListViewRowHeightRule rowHeightRule;
    private final SearchResultFromWidgetFactory fromWidgetfactory;
    private final NameRendererFactory nameRendererFactory;
    private final DownloadHandler downloadHandler;
    
    private RangeList<VisualSearchResult> maxSizedList;
    private ListEventListener<VisualSearchResult> maxSizedListener;
    
    private EventListJXTableSorting resultsTableSorting; 
    private ColorHighlighter resultsColorHighlighter;
    private Scrollable visibleComponent;

    /**
     * Constructs a BaseResultPanel with the specified components.
     */
    @AssistedInject
    public BaseResultPanel(
            @Assisted SearchResultsModel searchResultsModel,
            ResultsTableFormatFactory tableFormatFactory,
            ListViewTableEditorRendererFactory listViewTableEditorRendererFactory,
            Navigator navigator,
            PropertiesFactory<VisualSearchResult> properties, 
            ListViewRowHeightRule rowHeightRule,
            SearchResultFromWidgetFactory fromWidgetFactory,
            LibraryNavigator libraryNavigator,
            NameRendererFactory nameRendererFactory) {
        
        this.searchResultsModel = searchResultsModel;
        this.tableFormatFactory = tableFormatFactory;
        this.listViewTableEditorRendererFactory = listViewTableEditorRendererFactory;
        this.navigator = navigator;
        this.properties = properties;
        this.rowHeightRule = rowHeightRule;
        this.fromWidgetfactory = fromWidgetFactory;
        this.nameRendererFactory = nameRendererFactory;
        this.downloadHandler = new DownloadHandlerImpl(searchResultsModel, navigator, libraryNavigator);

        // Create tables.
        this.resultsList = createList();
        this.resultsTable = createTable();
        
        setLayout(layout);
 
        add(resultsList, SearchViewType.LIST.name());
        add(resultsTable, SearchViewType.TABLE.name());
    }
    
    /**
     * Creates a new List view table.
     */
    private ListViewTable createList() {
        ListViewTable listTable = new ListViewTable();
        
        // Set list table fields that do not change with search category.
        listTable.setShowGrid(true, false);
        listTable.setRowHeightEnabled(true);
        
        return listTable;
    }
    
    /**
     * Creates a new Table view table.
     */
    private ResultsTable<VisualSearchResult> createTable() {
        ResultsTable<VisualSearchResult> table = new ResultsTable<VisualSearchResult>();
        
        // Set table fields that do not change with search category.
        table.setPopupHandler(new SearchPopupHandler(table, downloadHandler, properties));
        table.setDoubleClickHandler(new ClassicDoubleClickHandler(table, downloadHandler));
        table.setRowHeight(TABLE_ROW_HEIGHT);
        
        return table;
    }
    
    /**
     * Configures the List view to display results for the selected category.
     */
    private void configureList() {
        // Remove listener with reference to previous list.
        if (maxSizedList != null) {
            maxSizedList.removeListEventListener(maxSizedListener);
        }
        
        // Get sorted list for selected category.
        final EventList<VisualSearchResult> sortedList = searchResultsModel.getSortedSearchResults();
        
        // Create sized list.
        maxSizedList = GlazedListsFactory.rangeList(sortedList);
        maxSizedList.setHeadRange(0, MAX_DISPLAYED_RESULT_SIZE + 1);
        
        // Create table format and set table model.
        ListViewTableFormat tableFormat = new ListViewTableFormat();
        resultsList.setEventListFormat(maxSizedList, tableFormat, false);
        
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
                return sortedList.size();
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
                searchResultsModel.getSearchQuery(), 
                navigator, downloadHandler, displayLimit);
        
        ListViewTableEditorRenderer editor = listViewTableEditorRendererFactory.create(
                searchResultsModel.getSearchQuery(), 
                navigator, downloadHandler, displayLimit);
        
        TableColumnModel tcm = resultsList.getColumnModel();
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setCellRenderer(renderer);
            tc.setCellEditor(editor);
        }
        
        resultsList.setDefaultEditor(VisualSearchResult.class, editor);

        // Set default width of all visible columns.
        for (int i = 0; i < tableFormat.getColumnCount(); i++) {
            resultsList.getColumnModel().getColumn(i).setPreferredWidth(tableFormat.getInitialWidth(i));
        }
        
        //add listener to table model to set row heights based on contents of the search results
        maxSizedListener = new ListEventListener<VisualSearchResult>() {
            @Override
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                
                EventTableModel tableModel = resultsList.getEventTableModel();
                if (tableModel.getRowCount() == 0) {
                    return;
                }
                
                //Push row resizing to the end of the event dispatch queue
                Runnable runner = new Runnable() {
                    @Override
                    public void run() {
                        EventTableModel model = resultsList.getEventTableModel();
                        
                        resultsList.setIgnoreRepaints(true);
                        boolean setRowSize = false;
                        for(int row = 0; row < model.getRowCount(); row++) {
                            VisualSearchResult vsr = (VisualSearchResult) model.getElementAt(row);
                            RowDisplayResult result = vsrToRowDisplayResultMap.get(vsr);
                            if (result == null || result.isStale(vsr)) {
                                result = rowHeightRule.getDisplayResult(vsr, searchResultsModel.getSearchQuery());
                                vsrToRowDisplayResultMap.put(vsr, result);
                            } 
                            int newRowHeight = result.getConfig().getRowHeight();
                            if(vsr.getSimilarityParent() == null) {
                                //only resize rows that belong to parent visual results.
                                //this will prevent the jumping when expanding child results as mentioned in
                                //https://www.limewire.org/jira/browse/LWC-2545
                                if (resultsList.getRowHeight(row) != newRowHeight) {
                                    LOG.debugf("Row: {0} vsr: {1} config: {2}", row, vsr.getHeading(), 
                                            result.getConfig());
                                    resultsList.setRowHeight(row, newRowHeight);
                                    setRowSize = true;
                                }
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
        };
        maxSizedList.addListEventListener(maxSizedListener);
        resultsList.setRowHeight(ROW_HEIGHT);        
    }

    /**
     * Configures the Table view to display results for the selected category.
     */
    private void configureTable() {
        // Uninstall components with references to previous list.
        if (resultsTableSorting != null) {
            resultsTableSorting.uninstall();
        }
        if (resultsColorHighlighter != null) {
            resultsTable.removeHighlighter(resultsColorHighlighter);
        }

        // Get results list and table format for selected category.
        SearchCategory selectedCategory = searchResultsModel.getSelectedCategory();
        EventList<VisualSearchResult> eventList = searchResultsModel.getCategorySearchResults(selectedCategory);
        ResultsTableFormat<VisualSearchResult> tableFormat = tableFormatFactory.createTableFormat(selectedCategory);

        // Create sorted list and set table model.
        SortedList<VisualSearchResult> sortedList = new SortedList<VisualSearchResult>(eventList);
        resultsTable.setEventListFormat(sortedList, tableFormat, true);

        //link the jxtable column headers to the sorted list
        resultsTableSorting = EventListJXTableSorting.install(resultsTable, sortedList, tableFormat);
            
        setupCellRenderers(tableFormat);
        
        // Apply column settings for table format.
        resultsTable.applySavedColumnSettings();

        TableColors tableColors = new TableColors();
        resultsColorHighlighter = new ColorHighlighter(new DownloadedHighlightPredicate(sortedList), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        resultsTable.addHighlighter(resultsColorHighlighter);
    }

    /**
     * Initializes cell renderers in the Table view column model based on 
     * column types provided by the specified table format. 
     */
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        SearchCategory selectedCategory = searchResultsModel.getSelectedCategory();
        
        CalendarRenderer calendarRenderer = new CalendarRenderer();
        TableCellRenderer nameRenderer = nameRendererFactory.createNameRenderer((selectedCategory == SearchCategory.ALL));
        TableCellRenderer defaultRenderer = new DefaultLibraryRenderer();
        
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Class clazz = tableFormat.getColumnClass(i);
            if (clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class) {
                setCellRenderer(i, defaultRenderer);
                setCellEditor(i, null);
            } else if (clazz == Calendar.class) {
                setCellRenderer(i, calendarRenderer);
                setCellEditor(i, null);
            } else if (i == tableFormat.getNameColumn()) {
                setCellRenderer(i, nameRenderer);
                setCellEditor(i, null);
            } else if (VisualSearchResult.class.isAssignableFrom(clazz)) {
                setCellRenderer(i, new FromTableCellRenderer(fromWidgetfactory.create(true)));
                setCellEditor(i, new FromTableCellRenderer(fromWidgetfactory.create(true)));
            }
        }
        
        // Set specific column renderers for selected category.
        switch (selectedCategory) {
        case ALL:
            setCellRenderer(AllTableFormat.SIZE_INDEX, new FileSizeRenderer());
            break;
        case AUDIO:
            setHeaderRenderer(AudioTableFormat.LENGTH_INDEX, new TableCellHeaderRenderer(JLabel.TRAILING));
            setCellRenderer(AudioTableFormat.SIZE_INDEX, new FileSizeRenderer());
            setCellRenderer(AudioTableFormat.LENGTH_INDEX, new TimeRenderer());
            setCellRenderer(AudioTableFormat.QUALITY_INDEX, new QualityRenderer());
            break;
        case VIDEO:
            setHeaderRenderer(VideoTableFormat.LENGTH_INDEX, new TableCellHeaderRenderer(JLabel.TRAILING));
            setCellRenderer(VideoTableFormat.SIZE_INDEX, new FileSizeRenderer());
            setCellRenderer(VideoTableFormat.LENGTH_INDEX, new TimeRenderer());
            setCellRenderer(VideoTableFormat.QUALITY_INDEX, new QualityRenderer());
            break;
        case DOCUMENT:
            setCellRenderer(DocumentTableFormat.SIZE_INDEX, new FileSizeRenderer());
            break;
        case IMAGE:
            setCellRenderer(ImageTableFormat.SIZE_INDEX, new FileSizeRenderer());
            break;
        case PROGRAM:
            setCellRenderer(ProgramTableFormat.SIZE_INDEX, new FileSizeRenderer());
            break;
        case OTHER:
            setCellRenderer(OtherTableFormat.SIZE_INDEX, new FileSizeRenderer());
            break;
        default:
            break;
        }
    }

    /**
     * Assigns the specified cell renderer to the specified column in the 
     * Table view column model.   
     */
    protected void setCellRenderer(int column, TableCellRenderer cellRenderer) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellRenderer(cellRenderer);
    }
    
    /**
     * Assigns the specified cell editor to the specified column in the 
     * Table view column model.   
     */
    protected void setCellEditor(int column, TableCellEditor editor) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellEditor(editor);
    }

    /**
     * Assigns the specified header renderer to the specified column in the 
     * Table view column model.   
     */
    protected void setHeaderRenderer(int column, TableCellRenderer headerRenderer) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setHeaderRenderer(headerRenderer);
    }

    /**
     * Displays search results for the specified search category.
     */
    public void showCategory(SearchCategory searchCategory) {
        // Select category to update sorted list.
        searchResultsModel.setSelectedCategory(searchCategory);
        
        // Configure results list and table.
        configureList();
        configureTable();
    }

    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setViewType(SearchViewType mode) {
        layout.show(this, mode.name());
        switch(mode) {
        case LIST: this.visibleComponent = resultsList; break;
        case TABLE: this.visibleComponent = resultsTable; break;
        default: throw new IllegalStateException("unsupported mode: " + mode);
        }
    }

    /**
     * Returns the header component for the scroll pane.  The method returns
     * null if no header is displayed.
     */
    public Component getScrollPaneHeader() {
        return visibleComponent == resultsTable ?
            resultsTable.getTableHeader() : null;
    }

    /**
     * Returns the results view component currently being displayed. 
     */
    public Scrollable getScrollable() {
        return visibleComponent;
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

    /**
     * Table component to display search results in a vertical list.
     */
    public static class ListViewTable extends ResultsTable<VisualSearchResult> {
        @Resource private Color similarResultParentBackgroundColor;        
        private boolean ignoreRepaints;
        
        public ListViewTable() {
            super();
            
            GuiUtils.assignResources(this);
            
            setGridColor(Color.decode("#EBEBEB"));
            setHighlighters(new ColorHighlighter(new HighlightPredicate() {
                public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                    VisualSearchResult vsr = (VisualSearchResult)getValueAt(adapter.row, 0);
                    return vsr != null && vsr.isChildrenVisible();
                }}, similarResultParentBackgroundColor, null, similarResultParentBackgroundColor, null));
        }
        
        @Override
        protected void paintEmptyRows(Graphics g) {
            // do nothing.
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
