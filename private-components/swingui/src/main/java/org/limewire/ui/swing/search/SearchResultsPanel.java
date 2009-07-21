package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.filter.AdvancedFilterPanel;
import org.limewire.ui.swing.filter.AdvancedFilterPanelFactory;
import org.limewire.ui.swing.filter.AdvancedFilterPanel.CategoryListener;
import org.limewire.ui.swing.friends.refresh.AllFriendsRefreshManager;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel.ListViewTable;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * This is the top-level container for the search results display.  
 * SearchResultsPanel contains several UI components, including the category
 * tab items, sort and filter panel, sponsored results panel, and search
 * results tables.
 */
public class SearchResultsPanel extends JXPanel implements SponsoredResultsView, Disposable {
    
    /** Decorator used to set the appearance of the header bar. */
    private final HeaderBarDecorator headerBarDecorator;
    
    /** Icon manager for categories. */
    private final CategoryIconManager categoryIconManager;
    
    /** Label that displays the search title. */
    private final JLabel searchTitleLabel = new JLabel();
    
    /** Panel containing filter components. */
    private final AdvancedFilterPanel<VisualSearchResult> filterPanel;
    
    /**
     * This is the subpanel that displays the actual search results.
     */
    private final ResultsContainer resultsContainer;
    
    /**
     * This is the subpanel that appears in the upper-right corner
     * of each search results tab.
     */
    private final SortAndFilterPanel sortAndFilterPanel;
    
    /** The sponsored results. */
    private final SponsoredResultsPanel sponsoredResultsPanel;
    
    /** The scroll pane embedding the search results & sponsored results. */
    private JScrollPane scrollPane;
    
    /** The ScrollablePanel that the scroll pane is embedding. */
    private ScrollablePanel scrollablePanel;
    
    /** The label where text about your search started poorly or later is written. */
    private JLabel messageLabel;
    /** The panel that displays the {@link #messageLabel}. */
    private JPanel messagePanel;
    
    /** The class search warning panel. */
    private ClassicSearchWarningPanel classicSearchReminderPanel;
    
    /** Listener for changes in the view type. */
    private final SettingListener viewTypeListener;
    
    /** Listener for updates to the result count. */
    private final ListEventListener<VisualSearchResult> resultCountListener;
    
    /** Search results data model. */
    private final SearchResultsModel searchResultsModel;
    
    @Resource private Color tabHighlightTopGradientColor;
    @Resource private Color tabHighlightBottomGradientColor;

    private boolean lifeCycleComplete = true;

    private boolean fullyConnected = true;

    private BrowseStatus browseStatus = null;
    
    /** Shows status of failed browses and refresh button.
     */
    private BrowseStatusPanel browseStatusPanel;
    
    /** Title when browsing friends; null for search results. */
    private String browseTitle;

    private final BrowseFailedMessagePanel browseFailedPanel;

    /**
     * Constructs a SearchResultsPanel with the specified components.
     */
    @Inject
    public SearchResultsPanel(
            @Assisted SearchResultsModel searchResultsModel,
            ResultsContainerFactory containerFactory,
            SortAndFilterPanelFactory sortAndFilterFactory,
            AdvancedFilterPanelFactory<VisualSearchResult> filterPanelFactory,
            SponsoredResultsPanel sponsoredResultsPanel,
            HeaderBarDecorator headerBarDecorator,
            CategoryIconManager categoryIconManager, 
            BrowseFailedMessagePanelFactory browseFailedMessagePanelFactory,
            AllFriendsRefreshManager allFriendsRefreshManager) {

        GuiUtils.assignResources(this);
        
        this.searchResultsModel = searchResultsModel;
        this.headerBarDecorator = headerBarDecorator; 
        this.categoryIconManager = categoryIconManager;
        
        this.sponsoredResultsPanel = sponsoredResultsPanel;
        this.sponsoredResultsPanel.setVisible(false);
        this.browseFailedPanel = browseFailedMessagePanelFactory.create(searchResultsModel);
        
        // Create sort and filter components.
        sortAndFilterPanel = sortAndFilterFactory.create(searchResultsModel);
        
        filterPanel = filterPanelFactory.create(searchResultsModel, searchResultsModel.getSearchType());
        
        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollablePanel = new ScrollablePanel();
        configureEnclosingScrollPane();
        
        // Create results container with tables.
        resultsContainer = containerFactory.create(searchResultsModel);
        
        viewTypeListener = new SettingListener() {
            int oldSearchViewTypeId = SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue();
            @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtilities.invokeLater(new Runnable() {
                   @Override
                   public void run() {
                       int newSearchViewTypeId = SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue();
                       if(newSearchViewTypeId != oldSearchViewTypeId) {
                           SearchViewType newSearchViewType = SearchViewType.forId(newSearchViewTypeId);
                           resultsContainer.setViewType(newSearchViewType);
                           syncScrollPieces();
                           oldSearchViewTypeId = newSearchViewTypeId;
                       }
                   }               
               });
            } 
        };
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.addSettingListener(viewTypeListener);
        
        // Initialize header label.
        updateTitle();
        
        // Install listener to update header label.
        resultCountListener = new ListEventListener<VisualSearchResult>() {
            @Override
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                updateTitle();
            }
        };
        searchResultsModel.getUnfilteredList().addListEventListener(resultCountListener);
        searchResultsModel.getFilteredList().addListEventListener(resultCountListener);
        
        // Configure sort panel and results container.
        sortAndFilterPanel.setSearchCategory(searchResultsModel.getSearchCategory());
        resultsContainer.showCategory(searchResultsModel.getSearchCategory());
        syncScrollPieces();
        
        // Configure advanced filters.
        filterPanel.setSearchCategory(searchResultsModel.getSearchCategory());
        filterPanel.addCategoryListener(new CategoryListener() {
            @Override
            public void categorySelected(SearchCategory displayCategory) {
                sortAndFilterPanel.setSearchCategory(displayCategory);
                resultsContainer.showCategory(displayCategory);
                syncScrollPieces();
                updateTitle();
            }
        });

        messageLabel = new JLabel();
        messagePanel = new JPanel();
        messagePanel.add(messageLabel);
        messagePanel.setVisible(false);
        
        browseStatusPanel = new BrowseStatusPanel(searchResultsModel, allFriendsRefreshManager);
        
        classicSearchReminderPanel = new ClassicSearchWarningPanel();
        layoutComponents();
    }

    /**
     * Disposes of resources used by the container.  This method is called when 
     * the search is closed.
     */
    @Override
    public void dispose() {
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.removeSettingListener(viewTypeListener);
        searchResultsModel.getFilteredList().removeListEventListener(resultCountListener);
        searchResultsModel.getUnfilteredList().removeListEventListener(resultCountListener);
        sortAndFilterPanel.dispose();
        filterPanel.dispose();
        classicSearchReminderPanel.dispose();
        browseFailedPanel.dispose();
        searchResultsModel.dispose();
        browseStatusPanel.dispose();
    }
    
    /**
     * @return the SearchResultsModel of the SearchResultsPanel.
     */
    public SearchResultsModel getModel(){
        return searchResultsModel;
    }

    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane() {
        JTableHeader th = new JTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        // Put a dummy header in the upper-right corner.
        final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
        JPanel cornerComponent = new JPanel(new BorderLayout());
        cornerComponent.add(renderer, BorderLayout.CENTER);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }
    
    /**
     * Adds the specified list of sponsored results to the display.
     */
    @Override
    public void addSponsoredResults(List<SponsoredResult> sponsoredResults){
        for (SponsoredResult result : sponsoredResults){
            sponsoredResultsPanel.addEntry(result);
        }
        
        if (!sponsoredResultsPanel.isVisible()) {
            sponsoredResultsPanel.setVisible(true);
            syncColumnHeader();
        }
    }
    
    /**
     * Sets the browse title in the container.  When not null, the browse title
     * is displayed at the top of the panel.  When null, the container displays 
     * the search title from the data model.
     */
    public void setBrowseTitle(String title) {
        browseTitle = title;
        updateTitle();
    }
    
    /**
     * Updates the title icon and text in the container.  For search results, 
     * the title includes the category name, search title, and result counts. 
     */
    private void updateTitle() {
        // Get result counts.
        int total = searchResultsModel.getUnfilteredList().size();
        int actual = searchResultsModel.getFilteredList().size();
        
        if (browseTitle != null) {
            // Set browse title.
            searchTitleLabel.setText((actual == total) ?
                    // {0}: browse title, {1}: total count
                    I18n.tr("Browse {0} ({1})", browseTitle, total) :
                    // {0}: browse title, {1}: actual count, {2}: total count 
                    I18n.tr("Browse {0} - Showing {1} of {2}", browseTitle, actual, total));
            
        } else {
            // Get search category and title.
            SearchCategory displayCategory = searchResultsModel.getSelectedCategory();
            String title = searchResultsModel.getSearchTitle();
            
            // Set title icon based on category.
            Icon icon = (displayCategory == SearchCategory.ALL) ? null :
                categoryIconManager.getIcon(displayCategory.getCategory());
            searchTitleLabel.setIcon(icon);

            // Set title text.
            switch (displayCategory) {
            case ALL:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("All results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("All results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case AUDIO:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Audio results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Audio results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case VIDEO:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Video results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Video results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case IMAGE:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Image results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Image results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case DOCUMENT:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Document results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Document results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case PROGRAM:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Program results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Program results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            case OTHER:
                searchTitleLabel.setText((actual == total) ?
                        // {0}: search title, {1}: total count
                        I18n.tr("Other results for {0} ({1})", title, total) :
                        // {0}: search title, {1}: actual count, {2}: total count 
                        I18n.tr("Other results for {0} - Showing {1} of {2}", title, actual, total));
                break;
            default:
                throw new IllegalStateException("Invalid search category " + displayCategory);
            }
        }
    }
    
    /**
     * Updates the column header component in the scroll pane.  This depends on
     * the current results view and whether the sponsored results are visible. 
     */
    private void syncColumnHeader() {
        Component resultHeader = resultsContainer.getScrollPaneHeader();
        if (resultHeader == null) {
            // If no headers, use nothing special.
            scrollPane.setColumnHeaderView(null);
        } else if (!sponsoredResultsPanel.isVisible()) {
            // If sponsored results aren't visible, just use the actual header.
            scrollPane.setColumnHeaderView(resultHeader);
        } else {
            // Otherwise, create a combined panel that has both sponsored results & header.
            JXPanel headerPanel = new JXPanel();
            // Make sure this syncs with the layout for the results & sponsored results!
            headerPanel.setLayout(new MigLayout("hidemode 3, gap 0, insets 0", "[]", "[grow][]"));
            headerPanel.add(resultHeader, "grow, push, alignx left, aligny top");
            
            DefaultTableColumnModel model = new DefaultTableColumnModel();
            TableColumn column = new TableColumn();
            model.addColumn(column);
            JTableHeader header = new JTableHeader(model);
            header.setDefaultRenderer(new TableCellHeaderRenderer());
            header.setReorderingAllowed(false);
            header.setResizingAllowed(false);
            header.setTable(new JXTable(0, 1));
            
            int width = sponsoredResultsPanel.getPreferredSize().width;
            int height = resultHeader.getPreferredSize().height;
            column.setWidth(width);
            Dimension dimension = new Dimension(width, height);
            header.setPreferredSize(dimension);
            header.setMaximumSize(dimension);
            header.setMinimumSize(dimension);
            
            headerPanel.add(header, "aligny top, alignx right");
            scrollPane.setColumnHeaderView(headerPanel);
        }
        
        scrollPane.validate();
        
        // Resize and repaint table header.  This eliminates visual issues due
        // to a change in the table format, which can result in an incorrect
        // header height or header flickering when a category is selected.
        if (resultHeader instanceof JTableHeader) {
            ((JTableHeader) resultHeader).resizeAndRepaint();
        }
    }
    
    /**
     * Initializes the components and adds them to the container.  Called by
     * the constructor.  
     */
    private void layoutComponents() {
        MigLayout layout = new MigLayout("hidemode 2, insets 0 0 0 0, gap 0!, novisualpadding", 
                                "[][grow]",       // col constraints
                                "[][][][grow]");  // row constraints
        
        setLayout(layout);
        setMinimumSize(new Dimension(getPreferredSize().width, 33));
        
        RectanglePainter tabHighlight = new RectanglePainter();
        tabHighlight.setFillPaint(new GradientPaint(20.0f, 0.0f, tabHighlightTopGradientColor, 
                                                    20.0f, 33.0f, tabHighlightBottomGradientColor));

        tabHighlight.setInsets(new Insets(0,0,1,0));
        tabHighlight.setBorderPaint(null);
        
        HeaderBar header = new HeaderBar(searchTitleLabel);
        header.setLayout(new MigLayout("insets 0, gap 0!, novisualpadding, alignx 100%, aligny 50%"));
        header.add(browseStatusPanel, "alignx 0%, growx, pushx");
        headerBarDecorator.decorateBasic(header);
        
        sortAndFilterPanel.layoutComponents(header);
        
        add(header                    , "spanx 2, growx, growy, wrap");
        add(classicSearchReminderPanel, "spanx 2, growx, wrap");
        add(messagePanel              , "spanx 2, growx, wrap");
        add(filterPanel, "grow");
        add(scrollPane , "hidemode 3, grow");
        add(browseFailedPanel , "hidemode 3, grow");

        scrollablePanel.setScrollableTracksViewportHeight(false);

        scrollablePanel.setLayout(new BorderLayout());
        scrollablePanel.add(resultsContainer, BorderLayout.CENTER);
        scrollablePanel.add(sponsoredResultsPanel, BorderLayout.EAST);
        scrollPane.setViewportView(scrollablePanel);

        
        syncScrollPieces();
    }

    /**
     * Updates the view components in the scroll pane. 
     */
    private void syncScrollPieces() {
        scrollablePanel.setScrollable(resultsContainer.getScrollable());
        syncColumnHeader();
    }
    
    /**
     * Panel used as the viewport view in the scroll pane.  This contains the
     * results table and sponsored results panel in a single, scrollable area.
     */
    private class ScrollablePanel extends JXPanel {
        private Scrollable scrollable;

        public void setScrollable(Scrollable scrollable) {
            this.scrollable = scrollable;
        }
        
        @Override
        public Dimension getPreferredSize() {
            if(scrollable == null) {
                return super.getPreferredSize();
            } else {
                int width = super.getPreferredSize().width;
                int height = ((JComponent)scrollable).getPreferredSize().height;
                
                // the list view has some weird rendering sometimes (double space after last result)
                // so don't fill full screen on list view
                if( (scrollable instanceof ListViewTable)) {
                    // old check, if sponsored results aren't showing properlly revert to just using this
                    if(sponsoredResultsPanel.isVisible()) {
                        height = Math.max(height, sponsoredResultsPanel.getPreferredSize().height);
                    }
                } else { // classic view
                    int headerHeight = 0;
                    
                    //the table headers aren't being set on the scrollpane, so if its visible check its
                    // height and subtract it from the viewport size
                    JTableHeader header = ((JTable)scrollable).getTableHeader();
                    if(header != null && header.isShowing()) {
                        headerHeight = header.getHeight();
                    }
                    
                    // if the height of table is less than the scrollPane height, set preferred height
                    // to same size as scrollPane
                    if(height < scrollPane.getSize().height - headerHeight) {
                        height = scrollPane.getSize().height - headerHeight;
                    }
                }
                return new Dimension(width, height);
            }
        }
        
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            if(scrollable == null) {
                return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
            } else {
                return scrollable.getScrollableUnitIncrement(visibleRect, orientation, direction);
            }
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            if(scrollable == null) {
                return super.getScrollableBlockIncrement(visibleRect, orientation, direction);
            } else {
                return scrollable.getScrollableBlockIncrement(visibleRect, orientation, direction);
            }
        }
    }

    /**
     * Sets an indicator to determine whether the application has finished
     * loading, and updates the user message.
     */
    public void setLifeCycleComplete(boolean lifeCycleComplete) {
        this.lifeCycleComplete = lifeCycleComplete;
        updateMessages();
    }

    /**
     * Sets an indicator to determine whether the application is fully 
     * connected to the P2P Network, and updates the user message.
     */
    public void setFullyConnected(boolean fullyConnected) {
        this.fullyConnected = fullyConnected;
        updateMessages();        
    }
    
    public void setBrowseStatus(BrowseStatus browseStatus){
        this.browseStatus = browseStatus;
        updateMessages();        
    }
    
    /**
     * Updates the user message based on the current state of the application. 
     */
    private void updateMessages() {
        browseStatusPanel.setBrowseStatus(browseStatus);
        
        if (!lifeCycleComplete) {
            messageLabel.setText(I18n.tr("LimeWire will start your search right after it finishes loading."));
            messagePanel.setVisible(true);
            browseFailedPanel.setVisible(false);
        } else if (!fullyConnected) {
            messageLabel.setText(I18n.tr("You might not receive many results until LimeWire finishes loading..."));
            messagePanel.setVisible(true);
            browseFailedPanel.setVisible(false);            
        } else if (browseStatus != null && !browseStatus.getState().isOK()) {
            browseFailedPanel.update(browseStatus.getState(), browseStatus.getBrowseSearch(), browseStatus.getFailedFriends());
            browseFailedPanel.setVisible(true);
        } else {
            messagePanel.setVisible(false);
            browseFailedPanel.setVisible(false);
        }

        filterPanel.setVisible(!browseFailedPanel.isVisible());
        scrollPane.setVisible(!browseFailedPanel.isVisible());
    }
}
