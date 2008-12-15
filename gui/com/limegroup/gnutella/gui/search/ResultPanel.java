package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.metal.MetalBorders;

import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.GUID;
import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.BrowseHostHandler;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.FileDetailsProvider;
import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconButton;
import com.limegroup.gnutella.gui.LicenseWindow;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.ProgTabUIFactory;
import com.limegroup.gnutella.gui.URLLabel;
import com.limegroup.gnutella.gui.actions.ActionUtils;
import com.limegroup.gnutella.gui.actions.SearchAction;
import com.limegroup.gnutella.gui.banner.Ad;
import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.dnd.MulticastTransferHandler;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.ColumnPreferenceHandler;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.TableSettings;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.gui.util.PopupUtils;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.VerificationListener;
import com.limegroup.gnutella.search.QueryHandler;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;


public class ResultPanel extends AbstractTableMediator<TableRowFilter, TableLine, SearchResult>
    implements VerificationListener, FileDetailsProvider {
    
    protected static final String SEARCH_TABLE = "SEARCH_TABLE";
    
    
    /** Flag that a search has been stopped with a random GUID */
    protected static final GUID STOPPED_GUID = new GUID(GUID.makeGuid());
    
    private static final DateRenderer DATE_RENDERER = new DateRenderer();
    private static final QualityRenderer QUALITY_RENDERER = new QualityRenderer();
    private static final EndpointRenderer ENDPOINT_RENDERER = new EndpointRenderer();
    private static final ResultSpeedRenderer RESULT_SPEED_RENDERER = new ResultSpeedRenderer();
    private static final PercentageRenderer PERCENTAGE_RENDERER = new PercentageRenderer();
    
    /**
     * The TableSettings that all ResultPanels will use.
     */
    static final TableSettings SEARCH_SETTINGS =
        new TableSettings("SEARCH_TABLE");
    
    /**
     * The search info of this class.
     */
    private final SearchInformation SEARCH_INFO;
    
    /**
     * This' spam filter
     */
    private final SpamFilter SPAM_FILTER;

    /**
     * The GUID of the last search. (Use this to match up results.)
     *  May be a DummyGUID for the empty result list hack.
     */
    protected volatile GUID guid;
    
    /**
     * The BrowseHostHandler if this is a Browse Host tab.
     */
    private BrowseHostHandler browseHandler = null;
    
    /**
     * Start time of the query that this specific ResultPane handles
     */
    private long startTime = System.currentTimeMillis();
    
    /**
     * The CompositeFilter for this ResultPanel.
     */
    CompositeFilter FILTER;
    
    /**
     * The download listener.
     */
    ActionListener DOWNLOAD_LISTENER;
    
    /**
     * The "download as" listener.
     */
    ActionListener DOWNLOAD_AS_LISTENER;
    
    /**
     * The chat listener.
     */
    ActionListener CHAT_LISTENER;
    
    /**
     * The browse host listener.
     */
    ActionListener BROWSE_HOST_LISTENER;
    
    /**
     * The stop listener.
     */
    ActionListener STOP_LISTENER;
    
    /**
     * The Mark As Spam listener
     */
    ActionListener MARK_AS_SPAM_LISTENER;
    
    /**
     * The Properties listener.
     */
    ActionListener PROPERTIES_LISTENER;
    
    /**
     * The Mark As Not Spam listener
     */
    ActionListener MARK_AS_NOT_SPAM_LISTENER;
    
    /**
     * The button that marks search results as spam or undoes it
     */
    private JButton SPAM_BUTTON;
    
    protected Box SOUTH_PANEL;


    private MouseInputListener ACTION_HIGHLIGHT_LISTENER;
    
    /** Quick'n'dirty counter for spam button clicks */
    @InspectablePrimitive("spam button clicks")
    private static volatile int spamClicks;
    
    @InspectionPoint("sortedResultColumns")
    private static final InspectionHistogram<String> sortedResultColumns = new InspectionHistogram<String>();
    
    /**
     * Specialized constructor for creating a "dummy" result panel.
     * This should only be called once at search window creation-time.
     */
    ResultPanel(JPanel overlay) {
        super(SEARCH_TABLE);
        setupFakeTable(overlay);
        SEARCH_INFO = SearchInformation.createKeywordSearch("", null,
                                      MediaType.getAnyTypeMediaType());
        SPAM_FILTER=null;
        FILTER = null;
        this.guid = STOPPED_GUID;
        setButtonEnabled(SearchButtons.STOP_BUTTON_INDEX, false);
        // disable dnd for overlay panel
        TABLE.setDragEnabled(false);
        TABLE.setTransferHandler(null);
        
        SOUTH_PANEL.setVisible(false);
    }
    
    /**
     * Constructor for creating a search panel with a given title.
     * This should be used for "pre-stopped" searches.
     */
    ResultPanel(String title, String id) {
        super(id);
        
        this.SPAM_FILTER = null;
        this.SEARCH_INFO = SearchInformation.createKeywordSearch(title, null, MediaType
                .getAnyTypeMediaType());
        
        this.guid = STOPPED_GUID;
    }   
   
    /**
     * Constructs a new ResultPanel for search results.
     *
     * @param guid the guid of the query.  Used to match results.
     * @param info the info of the search
     * @param resultStats used to track and access result stats.
     */
    ResultPanel(GUID guid, SearchInformation info) {
        super(SEARCH_TABLE);
        SEARCH_INFO = info;
        if (SEARCH_INFO.isBrowseHostSearch() || SEARCH_INFO.isWhatsNewSearch())
            SPAM_FILTER = null;
        else
            SPAM_FILTER = new SpamFilter();
        this.guid = guid;
        setupRealTable();
        resetFilters();
    }    
   
    /**
     * Sets the default renderers to be used in the table.
     */
    @Override
    protected void setDefaultRenderers() {
        super.setDefaultRenderers();
        TABLE.setDefaultRenderer(QualityHolder.class, QUALITY_RENDERER);
        TABLE.setDefaultRenderer(EndpointHolder.class, ENDPOINT_RENDERER);
        TABLE.setDefaultRenderer(ResultSpeed.class, RESULT_SPEED_RENDERER);
        TABLE.setDefaultRenderer(Date.class, DATE_RENDERER);
        TABLE.setDefaultRenderer(Float.class, PERCENTAGE_RENDERER);
        TABLE.setDefaultRenderer(ResultNameHolder.class, LINK_RENDERER);
    }

    /**
     * Does nothing.
     */
    @Override
    protected void updateSplashScreen() { }
    
    /**
     * Simple inner class to allow a PaddedPanel to implement Progressor. This
     * is necessary for the ProgTabUIFactory to get the percentage of its tabs.
     */
    private class PPP extends PaddedPanel
                      implements ProgTabUIFactory.Progressor {
        public double calculatePercentage(long now) {
            return ResultPanel.this.calculatePercentage(now);
        }
    }

    /**
     * Setup the data model 
     */
    protected void setupDataModel() {
        DATA_MODEL = new TableRowFilter(FILTER);
    }
    
    /**
     * Sets up the constants:
     * FILTER, MAIN_PANEL, DATA_MODEL, TABLE, BUTTON_ROW.
     */
    @Override
    protected void setupConstants() {

        FILTER = new CompositeFilter(4);
        MAIN_PANEL = new PPP();

        setupDataModel();        
        
        TABLE = new LimeJTable(DATA_MODEL) {
            /*
             * Override the line color methods to show special
             * colors for The LimeWire Store&#8482; song results.
             */
            
            @Override
            protected Color getEvenRowColor(int row) {
                return getLine(row).getEvenRowColor();
            }
            
            @Override
            protected Color getOddRowColor(int row) {
                return getLine(row).getOddRowColor();
            }
        };
        ((ResultPanelModel)DATA_MODEL).setTable(TABLE);
        BUTTON_ROW = new SearchButtons(this).getComponent();
        
        // The initialization of the SPAM_BUTTON is a bit
        // hackish. Use the NOT_SPAM label as it is longer
        // and needs thus more space. As next init the button
        // with the true label but keep the button width. See 
        // transformButton() for more info...
        SPAM_BUTTON = new IconButton(
                I18n.tr("Not Junk"), 
                "SEARCH_SPAM");
        transformSpamButton(I18n.tr("Junk"), 
                I18n.tr("Mark selected search results as Junk"));
        
        SPAM_BUTTON.setEnabled(false);
        SPAM_BUTTON.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TableLine[] lines = getAllSelectedLines();
                if (lines.length > 0) {
                    spamClicks++;
                    if (SpamFilter.isAboveSpamThreshold(lines[0])) {
                        MARK_AS_NOT_SPAM_LISTENER.actionPerformed(e);
                    } else {
                        MARK_AS_SPAM_LISTENER.actionPerformed(e);
                    }
                }
            }
        });    
    }
    
    
    @Override
    protected void setupDragAndDrop() {
    	TABLE.setDragEnabled(true);
        TABLE.setTransferHandler(new MulticastTransferHandler(new ResultPanelTransferHandler(this), DNDUtils.DEFAULT_TRANSFER_HANDLERS));
    }
    
    /**
     * Sets SETTINGS to be the static SEARCH_SETTINGS, instead
     * of constructing a new one for each ResultPanel.
     */
    @Override
    protected void buildSettings() {
        SETTINGS = SEARCH_SETTINGS;
    }
    
    /**
     * Creates the specialized SearchColumnSelectionMenu menu,
     * which groups XML columns together.
     */
    @Override
    protected JPopupMenu createColumnSelectionMenu() {
        return (new SearchColumnSelectionMenu(TABLE)).getComponent();
    }
    
    /**
     * Creates the specialized column preference handler for search columns.
     */
    @Override
    protected ColumnPreferenceHandler createDefaultColumnPreferencesHandler() {
        return new SearchColumnPreferenceHandler(TABLE);
    }    
    
    @Override
    protected void addListeners() {        
        super.addListeners();
        TABLE.addMouseMotionListener(ACTION_HIGHLIGHT_LISTENER);
        TABLE.addMouseListener(ACTION_HIGHLIGHT_LISTENER);
    }

    /** Sets all the listeners. */
    @Override
    protected void buildListeners() {
        super.buildListeners();
        
        ACTION_HIGHLIGHT_LISTENER = new MouseInputAdapter() {
            private final Cursor DEFAULT = Cursor.getDefaultCursor();
            private final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            private Cursor lastCursor = Cursor.getDefaultCursor();
            private boolean isCurrentlyHand;
            
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int column = TABLE.columnAtPoint(p);
                int colModel = TABLE.convertColumnIndexToModel(column);
                if(colModel == SearchTableColumns.NAME_IDX) {
                    int row = TABLE.rowAtPoint(p);
                    TableLine line = DATA_MODEL.get(row);
                    if(line != null && line.isLink()) {
                        if(lastCursor != HAND) {
                            lastCursor = HAND;
                            TABLE.getTopLevelAncestor().setCursor(HAND);
                            isCurrentlyHand = true;
                        }
                        return;
                    }       
                }
                
                if(lastCursor != DEFAULT) {
                    isCurrentlyHand = false;
                    lastCursor = DEFAULT;
                    TABLE.getTopLevelAncestor().setCursor(DEFAULT);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if(lastCursor != DEFAULT) {
                    isCurrentlyHand = false;
                    lastCursor = DEFAULT;
                    TABLE.getTopLevelAncestor().setCursor(DEFAULT);
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if(isCurrentlyHand) {
                    Point p = e.getPoint();
                    int column = TABLE.columnAtPoint(p);
                    int colModel = TABLE.convertColumnIndexToModel(column);
                    if(colModel == SearchTableColumns.NAME_IDX) {
                        int row = TABLE.rowAtPoint(p);
                        TableLine line = DATA_MODEL.get(row);
                        if(line != null && line.isLink()) {
                            SearchMediator.downloadFromPanel(ResultPanel.this, line);
                        }
                    }
                }
            }
        };
        
        DOWNLOAD_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SearchMediator.doDownload(ResultPanel.this);
                // Downloading a file implies the user thinks it's not spam
                for(TableLine line : getAllSelectedLines())
                    SPAM_FILTER.markAsSpamUser(line, false);
                DATA_MODEL.refresh();
                // This is harmless if the button's already in the right state
                transformSpamButton(I18n.tr("Junk"), 
                        I18n.tr("Mark selected search results as Junk"));
            }
        };
        
        DOWNLOAD_AS_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SearchMediator.doDownloadAs(ResultPanel.this);
            }
        };
        
        CHAT_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doChat();
            }
        };
        
        BROWSE_HOST_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SearchMediator.doBrowseHost(ResultPanel.this);
            }
        };
        
        STOP_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopSearch();
            }
        };

        PROPERTIES_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SearchMediator.showProperties(ResultPanel.this);
            }
        };

        MARK_AS_SPAM_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for(TableLine line : getAllSelectedLines())
                    SPAM_FILTER.markAsSpamUser(line, true);             
                
                // This is a bit fine tuning...
                if (SearchSettings.hideJunk()) {
                    filtersChanged();   // i.e. hide the search result(s) we've just
                                        // marked as spam
                } else {
                    DATA_MODEL.refresh(); // mark 'em red
                    transformSpamButton(I18n.tr("Not Junk"), 
                            I18n.tr("Mark selected search results as Not Junk"));
                }
            }
        };

        MARK_AS_NOT_SPAM_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for(TableLine line : getAllSelectedLines())
                    SPAM_FILTER.markAsSpamUser(line, false);
                DATA_MODEL.refresh();
                
                transformSpamButton(I18n.tr("Junk"), 
                        I18n.tr("Mark selected search results as Junk"));
            }
        };
    }
    
    /**
     * Creates the specialized SearchResultMenu for right-click popups.
     *
     * Upgraded access from protected to public for SearchResultDisplayer.
     */
    @Override
    public JPopupMenu createPopupMenu() {
        return createPopupMenu(getAllSelectedLines());
    }
    
    protected JPopupMenu createPopupMenu(TableLine[] lines) {
        //  do not return a menu if right-clicking on the dummy panel
        if (!isKillable())
            return null;
  

        JPopupMenu menu = new JPopupMenu();
        menu.add(createSearchAgainMenu(lines.length > 0 ? lines[0] : null));
        
        menu.addSeparator();
            
        PopupUtils.addMenuItem(SearchMediator.STOP_STRING, STOP_LISTENER, menu, !isStopped());
        PopupUtils.addMenuItem(SearchMediator.KILL_STRING, new CancelListener(), menu, isKillable());
        
        
        boolean allSpam = true;
        boolean allNot = true;
        
        if (SPAM_FILTER != null) {
            for (int i = 0; i < lines.length; i++) {
                if (!SpamFilter.isAboveSpamThreshold(lines[i]))
                    allSpam = false;
                else
                    allNot = false;
            }
        }

        return (new SearchResultMenu(this)).addToMenu(menu, lines, !allSpam, !allNot);
    }
    
    /**
     * Returns a menu with a 'repeat search' and 'repeat search no clear' action.
     */
    protected final JMenu createSearchAgainMenu(TableLine line) {
        JMenu menu = new JMenu(I18n.tr("Search More"));
        menu.add(new JMenuItem(new RepeatSearchAction()));
        menu.add(new JMenuItem(new RepeatSearchNoClearAction()));

        if (line == null) {
            menu.setEnabled(isRepeatSearchEnabled());
            return menu;
        }

        menu.addSeparator();
        String keywords = QueryUtils.createQueryString(line.getFilename());
        SearchInformation info = SearchInformation.createKeywordSearch(
                keywords, null, MediaType
                .getAnyTypeMediaType());
        if (SearchMediator.validateInfo(info) == SearchMediator.QUERY_VALID) {
            menu.add(new JMenuItem(new SearchAction(info, I18nMarker
                    .marktr("Search for Keywords: {0}"))));
        }

        LimeXMLDocument doc = line.getXMLDocument();
        if (doc != null) {
            Action[] actions = ActionUtils.createSearchActions(doc);
            for (int i = 0; i < actions.length; i++) {
                menu.add(new JMenuItem(actions[i]));
            }
        }

        return menu;
    } 
    
    
    /**
     * Do not allow removal of rows.
     */
    @Override
    public void removeSelection() { }
    
    /**
     * Clears the table and converts the download button into a
     * wishlist button.
     */
    @Override
    public void clearTable() {
        super.clearTable();
    }
    
    /**
     * Sets the appropriate buttons to be disabled.
     */
    public void handleNoSelection() {
        setButtonEnabled(SearchButtons.DOWNLOAD_BUTTON_INDEX, false);
        setButtonEnabled(SearchButtons.BROWSE_BUTTON_INDEX, false);
        
        SPAM_BUTTON.setEnabled(false);
        if (SearchSettings.ENABLE_SPAM_FILTER.getValue() && SPAM_FILTER != null) {
            transformSpamButton(I18n.tr("Junk"), 
                    I18n.tr("Mark selected search results as Junk"));
        }
    }
    
    /**
     * Sets the appropriate buttons to be enabled.
     */
    public void handleSelection(int i)  { 
        setButtonEnabled(SearchButtons.DOWNLOAD_BUTTON_INDEX, true);
        
        TableLine line = DATA_MODEL.get(i);
        setButtonEnabled(SearchButtons.BROWSE_BUTTON_INDEX,
                         line.isBrowseHostEnabled());
        
        // don't enable junk button for promotion items
        if( line.isLink() )
            SPAM_BUTTON.setEnabled(false);
        else if (SearchSettings.ENABLE_SPAM_FILTER.getValue() && SPAM_FILTER != null) {
            SPAM_BUTTON.setEnabled(true);
            
            if (SpamFilter.isAboveSpamThreshold(line)) {
                transformSpamButton(I18n.tr("Not Junk"), 
                        I18n.tr("Mark selected search results as Not Junk"));
            } else {
                transformSpamButton(I18n.tr("Junk"), 
                        I18n.tr("Mark selected search results as Junk"));
            }
        }
    }
    
    /**
     * Forwards the event to DOWNLOAD_LISTENER.
     */
    public void handleActionKey() {
        DOWNLOAD_LISTENER.actionPerformed(null);
    }
    
    /**
     * Gets the SearchInformation of this search.
     */
    SearchInformation getSearchInformation() {
        return SEARCH_INFO;
    }
    
    /**
     * Gets the query of the search.
     */
    String getQuery() {
        return SEARCH_INFO.getQuery();
    }
    
    /**
     * Returns the title of the search.
     * @return
     */
    String getTitle() {
        return SEARCH_INFO.getTitle();
    }
    
    /**
     * Gets the rich query of the search.
     */
    String getRichQuery() {
        return SEARCH_INFO.getXML();
    }    
    
    /**
     * Stops this result panel from receiving more results.
     */
    void stopSearch() {
        final GUID guidToStop = guid;
        BackgroundExecutorService.schedule(new Runnable() {
            public void run() {
                GuiCoreMediator.getSearchServices().stopQuery(guidToStop);
            }
        });
        setGUID(STOPPED_GUID);
        SearchMediator.checkToStopLime();
        setButtonEnabled(SearchButtons.STOP_BUTTON_INDEX, false);
    }
    

    /**
     * Chats with the host chat-enabled host in the selected
     * TableLine.
     */
    void doChat() {
        TableLine line = getSelectedLine();
        if(line == null)
            return;
        if(!line.isChatEnabled())
            return;
        line.doChat();
    }

    /**
     * Blocks the host that sent the selected result.
     */
    void blockHost() {
        TableLine[] lines = getAllSelectedLines();
        Set<String> uniqueHosts = new HashSet<String>();
        for (TableLine line : lines) {
        	uniqueHosts.add(line.getHostname());
        	for (SearchResult result : line.getOtherResults()) {
        		uniqueHosts.add(result.getHost());
        	}
        }
        
        int answer = GUIMediator.showConfirmListMessage(I18n.tr("Do you want to block search results from the following list of hosts?"), 
        		uniqueHosts.toArray(), JOptionPane.YES_NO_OPTION, null);
        if (answer == JOptionPane.YES_OPTION) {
            // FIXME move into SpamServicesImpl / IPFilter
            String[] bannedIps = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
            uniqueHosts.addAll(Arrays.asList(bannedIps));
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(uniqueHosts.toArray(new String[uniqueHosts.size()]));
            GuiCoreMediator.getSpamServices().reloadIPFilter();
        }
    }
    
    /**
     * Shows a LicenseWindow for the selected line.
     */
    void showLicense() {
        TableLine line = getSelectedLine();
        if(line == null)
            return;
            
        URN urn = line.getSHA1Urn();
        LimeXMLDocument doc = line.getXMLDocument();
        LicenseWindow window = LicenseWindow.create(line.getLicense(), urn, doc, this);
        GUIUtils.centerOnScreen(window);
        window.setVisible(true);
    }
    
    public void licenseVerified(License license) {
        // if it was valid at all, refresh.
        if(license.isValid(null))
            ((ResultPanelModel)DATA_MODEL).slowRefresh();
    }
    
    /**
     * Determines whether or not this panel is stopped.
     */
    boolean isStopped() {
        return guid.equals(STOPPED_GUID);
    }
    
    /**
     * Determines if this is empty.
     */
    boolean isEmpty() {
        return DATA_MODEL.getRowCount() == 0;
    }
    
    /**
     * Determines if this can be removed.
     */
    boolean isKillable() {
        // the dummy panel has a null filter, and is the only one not killable
        return FILTER != null;
    }
    
    /**
     * Notification that a filter on this panel has changed.
     *
     * Updates the data model with the new list, maintains the selection,
     * and moves the viewport to the first still visible selected row.
     *
     * Note that the viewport moving cannot be done by just storing the first
     * visible row, because after the filters change, the row might not exist
     * anymore.  Thus, it is necessary to store all visible rows and move to
     * the first still-visible one.
     */
    boolean filterChanged(TableLineFilter filter, int depth) {
        if(!FILTER.setFilter(depth, filter))
            return false;
        
        // store the selection & visible rows
        int[] rows = TABLE.getSelectedRows();
        TableLine[] lines = new TableLine[rows.length];
        List<TableLine> inView = new LinkedList<TableLine>();
        for(int i = 0; i < rows.length; i++) {
            int row = rows[i];
            TableLine line = DATA_MODEL.get(row);
            lines[i] = line;
            if(TABLE.isRowVisible(row))
                inView.add(line);
        }
        
        // change the table.
        DATA_MODEL.filtersChanged();
        
        // reselect & move the viewpoint to the first still visible row.
        for(int i = 0; i < rows.length; i++) {
            TableLine line = lines[i];
            int row = DATA_MODEL.getRow(line);
            if(row != -1) {
                TABLE.addRowSelectionInterval(row, row);
                if(inView != null && inView.contains(line)) {
                    TABLE.ensureRowVisible(row);
                    inView = null;
                }                    
            }
        }
        
        // update the tab count.
        SearchMediator.setTabDisplayCount(this);
        return true;
    }
    
    /**
     * Returns the total number of sources found for this search.
     */
    int totalSources() {
        return ((ResultPanelModel)DATA_MODEL).getTotalSources();
    }
    
    /**
     * Returns the total number of filtered source found for this search.
     */
    int filteredSources() {
        return DATA_MODEL.getFilteredSources();
    }

    /**
     * Determines whether or not repeat search is currently enabled.
     * Repeat search will be disabled if, for example, the original
     * search was performed too recently.
     *
     * @return <tt>true</tt> if the repeat search feature is currently
     *  enabled, otherwise <tt>false</tt>
     */
    boolean isRepeatSearchEnabled() {
        return FILTER != null;
    }

    void repeatSearch () {
      repeatSearch(true);
    }
    
    void repeatSearch (boolean clearTable) {
        if ( clearTable ) {
          clearTable();
          resetFilters();
        }
        startTime = System.currentTimeMillis();
        
        SearchMediator.setTabDisplayCount(this);
        SearchMediator.repeatSearch(this, SEARCH_INFO, clearTable);
        setButtonEnabled(SearchButtons.STOP_BUTTON_INDEX, true);
    }
    
    void resetFilters() {
        FILTER.reset();
        
        if (!SEARCH_INFO.isBrowseHostSearch() && !SEARCH_INFO.isWhatsNewSearch()) {
            DATA_MODEL.setJunkFilter(SPAM_FILTER);
        } else {
            DATA_MODEL.setJunkFilter(null);
        }
    }
    
    private void filtersChanged() {
        DATA_MODEL.filtersChanged();
        SearchMediator.setTabDisplayCount(this);
    }
    
    /**
     * Gets the MetadataModel used for results.
     */
    MetadataModel getMetadataModel() {
        return  ((ResultPanelModel)DATA_MODEL).getMetadataModel();
    }

    /** Returns true if this is responsible for results with the given GUID */
    boolean matches(GUID otherGuid) {
        return this.guid.equals(otherGuid);
    }

    /**
     * @modifies this
     * @effects sets this' guid.  This is needed for browse host functionality.
     */
    void setGUID(GUID guid) {
        this.guid=guid;
    }

    /** Returns the guid this is responsible for. */
    byte[] getGUID() {
        return guid.bytes();
    }

    /** Returns the media type this is responsible for. */
    MediaType getMediaType() {
        return SEARCH_INFO.getMediaType();
    }
    
    /**
     * Sets the BrowseHostHandler.
     */
    void setBrowseHostHandler(BrowseHostHandler bhh) {
        browseHandler = bhh;
    }
    
    /**
     * Gets all currently selected TableLines.
     * 
     * @return empty array if no lines are selected.
     */
    TableLine[] getAllSelectedLines() {
        int[] rows = TABLE.getSelectedRows();
        if(rows == null)
            return new TableLine[0];
        
        TableLine[] lines = new TableLine[rows.length];
        for(int i = 0; i < rows.length; i++)
            lines[i] = DATA_MODEL.get(rows[i]);
        return lines;
    }
    
    /**
     * Gets the currently selected TableLine.
     * 
     * @return null if there is no selected line.
     */
    TableLine getSelectedLine() {
        int selected = TABLE.getSelectedRow();
        if(selected != -1) 
            return DATA_MODEL.get(selected);
        else
            return null;
    }
    
    /**
     * Gets the TableLine at <code>index</code>
     * 
     * @param index index of the line you want
     * @return null if there is no selected line.
     */
    final TableLine getLine(int index) {
        return DATA_MODEL.get(index);
    }      
    
    /**
     * Calculates the percentange of results that have been received for this
     * ResultPanel.
     */
    double calculatePercentage(long currentTime) {
        if(guid.equals(STOPPED_GUID))
            return 1d;

        if(SEARCH_INFO.isBrowseHostSearch()) {
            if( browseHandler != null )
                return browseHandler.getPercentComplete(currentTime);
            else
                return 0d;
        } 
        
        // first calculate the percentage solely based on 
        // the number of results we've received.
        int ideal = QueryHandler.ULTRAPEER_RESULTS;
        double resultPerc = (double)totalSources() / ideal;
        
        // then calculate the percentage solely based on
        // the time we've spent querying.
        long spent = currentTime - startTime;
        double timePerc = (double)spent / QueryHandler.MAX_QUERY_TIME;
        
        // If the results are already enough to fill it up, just use that.
        if( resultPerc >= 1 )
            return 1d;
        
        // Otherwise, the time percentage should fill up what remains in
        // the progress.
        timePerc = timePerc * (1 - resultPerc);
        
        // Return the results received + time spent.
        return resultPerc + timePerc;
    }            
    
    /**
     * Sets extra values for non dummy ResultPanels.
     * (Used for all tables that will have results.)
     *
     * Currently:
     * - Sorts the count column, if it is visible & real-time sorting is on.
     * - Adds listeners, so the filters can be displayed when necessary.
     */
    private void setupRealTable() {
        SearchTableColumns columns =
            ((ResultPanelModel)DATA_MODEL).getColumns();
        LimeTableColumn countColumn =
            columns.getColumn(SearchTableColumns.COUNT_IDX);
        if(SETTINGS.REAL_TIME_SORT.getValue() &&
           TABLE.isColumnVisible(countColumn.getId())) {
            DATA_MODEL.sort(SearchTableColumns.COUNT_IDX); // ascending
            DATA_MODEL.sort(SearchTableColumns.COUNT_IDX); // descending
        }
        
        MouseListener filterDisplayer = new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if(e.isConsumed())
                    return;
                e.consume();
                SearchMediator.panelSelected(ResultPanel.this);
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        };
        // catches around the button area.
        MAIN_PANEL.addMouseListener(filterDisplayer);
        // catches the blank area before results fill in
        SCROLL_PANE.addMouseListener(filterDisplayer);
        // catches selections on the table
        TABLE.addMouseListener(filterDisplayer);
        // catches the table header
        TABLE.getTableHeader().addMouseListener(filterDisplayer);
    }
    
    
    protected void setupMainPanelBase() {
        if (SearchSettings.ENABLE_SPAM_FILTER.getValue() 
                && MAIN_PANEL != null) {
            MAIN_PANEL.add(getScrolledTablePane());
            addButtonRow();
            MAIN_PANEL.setMinimumSize(ZERO_DIMENSION);
        } else {
            super.setupMainPanel();
        }
    }
    
    /**
     * Overwritten
     */
    @Override
    protected void setupMainPanel() {
        MAIN_PANEL.add(createSecurityWarning());
        
        setupMainPanelBase();
    }
           
    private JComponent createSecurityWarning() {
        // Only trust search results with a {0} to be official LimeWire results!
        Ad ad = SearchMediator.getAd();
        String wholeWarning = ad.getText();
        String warningOne = "Only search results with a";
        String warningTwo = "are official LimeWire communications.";
        int lockIdx = wholeWarning.indexOf("{0}");
        if(lockIdx != -1) {
            warningOne = wholeWarning.substring(0, lockIdx-1);
            warningTwo = wholeWarning.substring(lockIdx+4);
        }

        String moreInfo = I18n.tr("More Info...");
        
        JPanel jp = createWarningDitherPanel();
        jp.setLayout((new FlowLayout(FlowLayout.LEFT, 3, 3)));
        jp.add(new JLabel(GUIMediator.getThemeImage("warn-triangle")));
        jp.add(new JLabel("<html><font color=\"#7B5100\"><b>" + warningOne + "</b></font></html>"));
        jp.add(new JLabel(GUIMediator.getThemeImage("limehires")));
        jp.add(new JLabel("<html><font color=\"#7B5100\"><b>" + warningTwo + "</b></font></html>"));
        jp.add(Box.createHorizontalStrut(2));
        URLLabel urlLabel = new URLLabel(LimeWireUtils.addLWInfoToUrl(ad.getURI(), GuiCoreMediator.getApplicationServices().getMyGUID()),
                "<b>" + moreInfo + "</b>") ;
        urlLabel.setColor(new Color(0xAC,0x71,0x00));
        jp.add(urlLabel);

        Dimension ps = jp.getPreferredSize();
        ps.width = Short.MAX_VALUE;
        jp.setMaximumSize(ps);
        
        return jp;
    }
    
    protected JPanel createWarningDitherPanel() {
        return new DitherPanel(new Ditherer(6, new Color(255, 209, 86), new Color(255, 183, 44))) {
            @Override
            public void updateUI() {
                super.updateUI();

                Border border = UIManager.getBorder("ScrollPane.border");
                if (border != null && border.getClass() == MetalBorders.ScrollPaneBorder.class) {
                    setBorder(new WarningBorder(UIManager.getColor("SplitPane.darkShadow"), 1));
                } else {
                    if (OSUtils.isMacOSX()) {
                        setBorder(new WarningBorder(Color.lightGray, 0));
                    } else {
                        setBorder(new WarningBorder(UIManager.getColor("TextField.darkShadow"), 0));
                    }
                }
            }
        };
    }

    /**
     * Adds the overlay panel into the table & converts the button
     * to 'download'.
     */
    private void setupFakeTable(JPanel overlay) {
        MAIN_PANEL.removeAll();
        
        JPanel background = new JPanel();
        background.setLayout(new OverlayLayout(background));
        JPanel overlayPanel = new BoxPanel(BoxPanel.Y_AXIS);
        overlayPanel.setOpaque(false);
        overlayPanel.add(Box.createVerticalStrut(20));
        overlayPanel.add(overlay);
        overlayPanel.setMinimumSize(new Dimension(0, 0));
        JComponent table = getScrolledTablePane();
        table.setOpaque(false);
        background.add(overlayPanel);
        background.add(table);
        
        MAIN_PANEL.add(background);
        addButtonRow();
        
        MAIN_PANEL.setMinimumSize(ZERO_DIMENSION);
    }
    
    /**
     * Adds the button row and the Spam Button
     */
    private void addButtonRow() {
        if (BUTTON_ROW != null) {
            SOUTH_PANEL = Box.createVerticalBox();
            SOUTH_PANEL.setOpaque(false);
            
            SOUTH_PANEL.add(Box.createVerticalStrut(GUIConstants.SEPARATOR));
            
            if (SearchSettings.ENABLE_SPAM_FILTER.getValue() && SPAM_BUTTON != null) {
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new GridBagLayout());
                GridBagConstraints gbc = null;
                
                gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridwidth = GridBagConstraints.RELATIVE;
                gbc.weightx = 1;
                buttonPanel.add(BUTTON_ROW, gbc);
                
                gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                buttonPanel.add(SPAM_BUTTON, gbc);
                
                buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
                SOUTH_PANEL.add(buttonPanel);
            } else {
                SOUTH_PANEL.add(BUTTON_ROW);
            }
            
            MAIN_PANEL.add(SOUTH_PANEL);
        }
    }
    
    public FileDetails[] getFileDetails() {
        int[] sel = TABLE.getSelectedRows();
        List<FileDetails> list = new ArrayList<FileDetails>(sel.length);
        for (int i = 0; i < sel.length; i++) {
            TableLine line = DATA_MODEL.get(sel[i]);
            // prefer non-firewalled rfds for the magnet action
            RemoteFileDesc rfd = line.getNonFirewalledRFD();
            
            if (rfd != null) {
                list.add(rfd);
            }
            else {
                // fall back on first rfd
                rfd = line.getRemoteFileDesc();
                if (rfd != null) {
                    list.add(rfd);
                }
            }
        }
        if (list.isEmpty()) {
            return new FileDetails[0];
        }
        return list.toArray(new FileDetails[0]);
    }
    
    public void cleanup() {
    }
    
    @Override
    protected void sortAndMaintainSelection(int columnToSort) {
        if (columnToSort != -1) {
            LimeTableColumn column = DATA_MODEL.getTableColumn(columnToSort);
            sortedResultColumns.count(column.getId());
        }
        super.sortAndMaintainSelection(columnToSort);
    }

    /**
     * Change the text and tooltip text of the SPAM_BUTTON
     */
    private void transformSpamButton(String text, String tip) {
        Dimension oldDim = SPAM_BUTTON.getPreferredSize();
        
        SPAM_BUTTON.setText(text);
        SPAM_BUTTON.setToolTipText(tip);
        
        // Preserve/use the max width...
        Dimension newDim = SPAM_BUTTON.getPreferredSize();
        newDim.width = Math.max(oldDim.width, newDim.width);
        SPAM_BUTTON.setPreferredSize(newDim);
    }
    
    public class WarningBorder extends AbstractBorder {
     
        private Color lineColor;
        private int offset;

        public WarningBorder(Color lineColor, int offset) {
            this.lineColor = lineColor;
            this.offset = offset;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y,
                int width, int height) {
            Color oldColor = g.getColor();

            g.setColor(lineColor);

            // top
            g.drawLine(x, y, x + width - 1 - offset, y);
            // left
            g.drawLine(x, y, x, y + height);
            // right
            g.drawLine(x + width - 1 - offset, y, x + width - 1 - offset, y + height);
            
            if (offset > 0) {
                g.setColor(c.getParent().getBackground());
                for (int i = 0; i < offset; i++) {
                    g.drawLine(x + width - 1 - i, y, x + width - 1 - i, y + height);                    
                }
            }
            
            g.setColor(oldColor);
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 0, 1 + offset);
        }
        
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }


    private final class RepeatSearchAction extends AbstractAction {

        public RepeatSearchAction() {
            putValue(Action.NAME, SearchMediator.REPEAT_SEARCH_STRING);
            setEnabled(isRepeatSearchEnabled());
        }

        public void actionPerformed(ActionEvent e) {
            repeatSearch(true);
        }
    }

    private final class RepeatSearchNoClearAction extends AbstractAction {

        public RepeatSearchNoClearAction() {
            putValue(Action.NAME, SearchMediator.REPEAT_SEARCH_NO_CLEAR_STRING);
            setEnabled(isRepeatSearchEnabled());
        }

        public void actionPerformed(ActionEvent e) {
            repeatSearch(false);
        }
    }

    /**
     * Cancels the search.
     */
    protected final class CancelListener extends AbstractAction {
        
        public void actionPerformed(ActionEvent e) {
            SearchMediator.killSearch();
        }
    }  

}
