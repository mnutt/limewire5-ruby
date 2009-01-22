package org.limewire.ui.swing.search.resultpanel.list;

import static org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig.HeadingSubHeadingAndMetadata;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.EventObject;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ActionButtonPanel;
import org.limewire.ui.swing.search.resultpanel.ActionColumnTableCellEditor;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;
import org.limewire.ui.swing.search.resultpanel.SearchHeading;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidget;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidgetFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenu;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.PropertyMatch;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.OSUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class is responsible for rendering an individual SearchResult
 * in "List View".
 */
public class ListViewTableEditorRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

    private static final int LEFT_COLUMN_WIDTH = 450;

    private final CategoryIconManager categoryIconManager;
    
    private static final String HTML = "<html>";
    private static final String CLOSING_HTML_TAG = "</html>";
    private final Log LOG = LogFactory.getLog(getClass());

    private final String searchText;
    @Resource private Icon similarResultsIcon;
    @Resource private Color subHeadingLabelColor;
    @Resource private Color metadataLabelColor;
    @Resource private Color downloadSourceCountColor;
    @Resource private Color similarResultsBackgroundColor;
    @Resource private Color surplusRowLimitColor;
    @Resource private Font headingFont;
    @Resource private Font subHeadingFont;
    @Resource private Font metadataFont;
    @Resource private Font downloadSourceCountFont;
    @Resource private Font surplusRowLimitFont;
    @Resource private Icon spamIcon;
    @Resource private Icon downloadingIcon;
    @Resource private Icon libraryIcon;
    @Resource private Icon propertiesDown;
    @Resource private Icon propertiesHover;
    @Resource private Icon propertiesNormal;
    @Resource private Icon similarUpIcon;
    @Resource private Icon similarDownIcon;
    @Resource private Icon similarActiveIcon;
    @Resource private Icon similarHoverIcon;
    @Resource private Icon dividerIcon;
    
    private final ActionColumnTableCellEditor actionEditor;
    private final SearchHeadingDocumentBuilder headingBuilder;
    private final ListViewRowHeightRule rowHeightRule;
    private final ListViewDisplayedRowsLimit displayLimit;
    private final SearchResultTruncator truncator;
    private final HeadingFontWidthResolver headingFontWidthResolver = new HeadingFontWidthResolver();
    private final IconManager iconManager;
    private ActionButtonPanel actionButtonPanel;
    private SearchResultFromWidget fromWidget;
    private JLabel itemIconLabel;
    private JToggleButton similarButton = new JToggleButton();
    private JButton propertiesButton = new JButton();
    private JEditorPane heading = new JEditorPane();
    private JLabel subheadingLabel = new JLabel();
    private JLabel metadataLabel = new JLabel();
    private JLabel downloadSourceCount = new JLabel();
    private JXPanel editorComponent;

    private VisualSearchResult vsr;
    private JTable table;
    private JComponent similarResultIndentation;
    private JPanel indentablePanel;
    private JPanel leftPanel;
    private JPanel fromPanel;
    private JPanel lastRowPanel;
    private final JPanel emptyPanel = new JPanel();
    private JXPanel searchResultTextPanel;

    private int currentColumn;
    private int currentRow;
    private int mousePressedRow = -1;
    private int mousePressedColumn = -1;

    private JLabel lastRowMessage;

    @AssistedInject
    ListViewTableEditorRenderer(
            CategoryIconManager categoryIconManager,
            SearchResultFromWidgetFactory fromWidgetFactory,
        @Assisted ActionColumnTableCellEditor actionEditor, 
        @Assisted String searchText, 
        @Assisted Navigator navigator, 
        final @Assisted Color rowSelectionColor,
        final @Assisted DownloadHandler downloadHandler,
        SearchHeadingDocumentBuilder headingBuilder,
        ListViewRowHeightRule rowHeightRule,
        final PropertiesFactory<VisualSearchResult> properties,
        final @Assisted ListViewDisplayedRowsLimit displayLimit,
        LibraryNavigator libraryNavigator,
        SearchResultTruncator truncator, IconManager iconManager) {

        this.categoryIconManager = categoryIconManager;
        
        this.actionEditor = actionEditor;
        this.searchText = searchText;
        this.headingBuilder = headingBuilder;
        this.rowHeightRule = rowHeightRule;
        this.displayLimit = displayLimit;
        this.truncator = truncator;
        this.iconManager = iconManager;
        GuiUtils.assignResources(this);

        similarButton.setPressedIcon(similarDownIcon);
        similarButton.setIcon(similarUpIcon);
        similarButton.setToolTipText(I18n.tr("Show similar files"));
        similarButton.setBorderPainted(false);
        similarButton.setContentAreaFilled(false);
        //This mouse listener is unfortunately necessary. Setting the icons for rollover
        //causes the wrong icon to be displayed after the button is active and the 
        //mouse has entered "edit" mode in the cell.
        similarButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                similarButton.setIcon(isShowingSimilarResults() ? similarActiveIcon : similarUpIcon);
                similarButton.setToolTipText(isShowingSimilarResults() ? 
                        I18n.tr("Hide similar files") : I18n.tr("Show similar files"));
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                if (!isShowingSimilarResults()) {
                    similarButton.setIcon(similarHoverIcon);
                }
            }

            @Override
            public void mouseExited(MouseEvent arg0) { 
                if (!isShowingSimilarResults()) {
                    similarButton.setIcon(similarUpIcon);
                }
            }
        });

        propertiesButton.setIcon(propertiesNormal);
        propertiesButton.setRolloverIcon(propertiesHover);
        propertiesButton.setRolloverEnabled(true);
        propertiesButton.setPressedIcon(propertiesDown);
        propertiesButton.setToolTipText(I18n.tr("View file info"));
        propertiesButton.setBorderPainted(false);
        propertiesButton.setContentAreaFilled(false);
        propertiesButton.setFocusPainted(false);
        
        fromWidget = fromWidgetFactory.create(false);
       
        makePanel(navigator, libraryNavigator, properties);
        
        editorComponent = new JXPanel(new BorderLayout()) {

            @Override
            public void setBackground(final Color bg) {
                //Don't highlight the limit row
                if (currentRow == displayLimit.getLastDisplayedRow()) {
                    super.setBackground(Color.WHITE);
                    return;
                }
                boolean editingColumn = mousePressedColumn == currentColumn;
                boolean editingRow = mousePressedRow == currentRow;
                boolean paintForCellSelection = editingColumn && editingRow;
                super.setBackground(paintForCellSelection ? rowSelectionColor : bg);
                if (paintForCellSelection) {
                    mousePressedColumn = -1;
                    mousePressedRow = -1;
                }
            }
        };
        
        itemIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e) && isDownloadEligible(vsr)) {
                    actionButtonPanel.startDownload();
                    table.editingStopped(new ChangeEvent(table));
                }
            }
        });
        
        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                properties.newProperties().showProperties(vsr);
            }
        });
        
        MousePopupListener popupListener = new MousePopupListener() {
            @Override
            public void handlePopupMouseEvent(MouseEvent e) {
                final VisualSearchResult result = vsr; 
                SearchResultMenu searchResultMenu = new SearchResultMenu(downloadHandler, Collections.singletonList(vsr), properties);
                searchResultMenu.addPopupMenuListener(new PopupMenuListener() {
                    @Override
                    public void popupMenuCanceled(PopupMenuEvent e) {
                        result.setShowingContextOptions(false);
                    }

                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                        result.setShowingContextOptions(false);
                        table.editingStopped(new ChangeEvent(this));
                    }

                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                        result.setShowingContextOptions(true);
                    }
                });
                searchResultMenu.show(editorComponent, e.getX()+3, e.getY()+3);
            }
        };
        
        editorComponent.addMouseListener(popupListener);
        itemIconLabel.addMouseListener(popupListener);
        heading.addMouseListener(popupListener);
    }

    public Object getCellEditorValue() {
        return vsr;
    }

    private int getSimilarResultsCount() {
        return vsr == null ? 0 : vsr.getSimilarResults().size();
    }
    
    @Override
    public boolean isCellEditable(EventObject e) {
        //TODO this is probably unnecessary since MouseableTable handles background color
        if (table != null && e instanceof MouseEvent) {
            MouseEvent event = (MouseEvent) e;
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                //Cache the cell that's just been clicked on so that the editor component
                //can draw the correct selection background color 
                mousePressedRow = table.rowAtPoint(event.getPoint());
                mousePressedColumn = table.columnAtPoint(event.getPoint());
            }
        }
        return super.isCellEditable(e);
    }

    @Override
    public Component getTableCellEditorComponent(
        final JTable table, Object value, boolean isSelected, int row, final int col) {
        vsr = (VisualSearchResult) value;
        this.table = table;
        LOG.debugf("getTableCellEditorComponent: row = {0} column = {1}", row, col);

        currentColumn = col;
        currentRow = row;
        
        if (value == null) {
            editorComponent.removeAll();
            editorComponent.add(emptyPanel, BorderLayout.CENTER);
            return editorComponent;
        }
        
        actionButtonPanel =
            (ActionButtonPanel) actionEditor.getTableCellEditorComponent(
                    table, value, isSelected, row, col);

        LOG.debugf("row: {0} shouldIndent: {1}", row, vsr.getSimilarityParent() != null);
        
        editorComponent.removeAll();
        JPanel panel = null;
        if (row == displayLimit.getLastDisplayedRow()) {
            lastRowMessage.setText(tr("Not showing {0} results", 
                    (displayLimit.getTotalResultsReturned() - displayLimit.getLastDisplayedRow())));
            panel = col == 0 ? lastRowPanel : emptyPanel;
        } else {
            populatePanel((VisualSearchResult) value, col);
            panel = col == 0 ? leftPanel : fromPanel;
            
            if (col == 1) {
                propertiesButton.setIcon(propertiesNormal);
                similarButton.setIcon(isShowingSimilarResults() ? similarActiveIcon : similarUpIcon);
                similarButton.setToolTipText(isShowingSimilarResults() ? 
                        I18n.tr("Hide similar files") : I18n.tr("Show similar files"));
            }
        }

        editorComponent.add(panel, BorderLayout.CENTER);

        return editorComponent;
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        return getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }
    
    private boolean isShowingSimilarResults() {
        return vsr != null && getSimilarResultsCount() > 0 && vsr.isChildrenVisible();
    }

    private JPanel makeFromPanel() {
        similarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                vsr.toggleChildrenVisibility();
            }
        });
        
        JXPanel panel = new JXPanel(new MigLayout("", "0[][][]8[]", "5[]")) {
            @Override
            public void setBackground(Color color) {
                super.setBackground(color);
                for (Component component : getComponents()) {
                    component.setBackground(color);
                }
            }
        };

        panel.setOpaque(false);
        panel.add(new JLabel(dividerIcon));
        panel.add(fromWidget, "push");
        //Tweaked the width of the icon because display gets clipped otherwise
        panel.add(similarButton, "hmax 25, wmax 27");
        panel.add(propertiesButton, "hmax 25, wmax 27");

        return panel;
    }

    private Component makeLeftPanel(final Navigator navigator, final LibraryNavigator libraryNavigator) {
        itemIconLabel = new JLabel();
        itemIconLabel.setOpaque(false);
        
        heading.setContentType("text/html");
        heading.setEditable(false);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        heading.setOpaque(false);
        
        heading.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                populateHeading(rowHeightRule.getDisplayResult(vsr, searchText), vsr.getDownloadState(), true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                populateHeading(rowHeightRule.getDisplayResult(vsr, searchText), vsr.getDownloadState(), false);
            }
        });

        subheadingLabel.setForeground(subHeadingLabelColor);
        subheadingLabel.setFont(subHeadingFont);
        
        metadataLabel.setForeground(metadataLabelColor);
        metadataLabel.setFont(metadataFont);
        
        downloadSourceCount.setForeground(downloadSourceCountColor);
        downloadSourceCount.setFont(downloadSourceCountFont);
        
        JXPanel itemIconPanel = new JXPanel(new MigLayout("insets 7 0 0 5", "0[]", "0[]0"));
        itemIconPanel.setOpaque(false);
        itemIconPanel.add(itemIconLabel);

        searchResultTextPanel = new JXPanel(new MigLayout("fill, insets 0", "0[fill]", "[]0[]0[]"));
        searchResultTextPanel.setOpaque(false);
        searchResultTextPanel.add(heading, "wrap, growx");
        searchResultTextPanel.add(subheadingLabel, "wrap");
        searchResultTextPanel.add(metadataLabel);

        JXPanel panel = new JXPanel(new MigLayout("fill, insets 0 0 0 0", "5[][fill][]5", "0[]0"));
        panel.setOpaque(false);

        panel.add(itemIconPanel);
        panel.add(searchResultTextPanel, "push");
        panel.add(downloadSourceCount, "gapbottom 3");
        
        heading.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (EventType.ACTIVATED == e.getEventType()) {
                    if (e.getDescription().equals("#download")) {
                        actionButtonPanel.startDownload();
                        table.editingStopped(new ChangeEvent(table));
                    } else if (e.getDescription().equals("#downloading")) {
                        navigator.getNavItem(
                            NavCategory.DOWNLOAD,
                            MainDownloadPanel.NAME).select(vsr);
                    } else if (e.getDescription().equals("#library")) {
                        libraryNavigator.selectInLibrary(vsr.getUrn(), vsr.getCategory());
                    }
                }
            }
        });

        return panel;
    }

    private Cursor getIconCursor(VisualSearchResult vsr) {
        boolean useDefaultCursor = !isDownloadEligible(vsr);
        return Cursor.getPredefinedCursor(useDefaultCursor ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR);
    }

    private boolean isDownloadEligible(VisualSearchResult vsr) {
        return !vsr.isSpam() && vsr.getDownloadState() == BasicDownloadState.NOT_STARTED;
    }

    private void makePanel(Navigator navigator, LibraryNavigator libraryNavigator, PropertiesFactory<VisualSearchResult> properties) {
        leftPanel = makeIndentablePanel(makeLeftPanel(navigator, libraryNavigator));

        fromPanel = makeFromPanel();

        lastRowPanel = new JPanel(new MigLayout("insets 10 30 0 0", "[]", "[]"));
        lastRowPanel.setOpaque(false);
        lastRowMessage = new JLabel();
        lastRowMessage.setFont(surplusRowLimitFont);
        lastRowMessage.setForeground(surplusRowLimitColor);
        lastRowPanel.add(lastRowMessage);
        emptyPanel.setOpaque(false);
    }

    private JPanel makeIndentablePanel(Component component) {
        indentablePanel = new JPanel(new BorderLayout());
        indentablePanel.setOpaque(false);
        similarResultIndentation = new JPanel(new BorderLayout());
        similarResultIndentation.add(new JLabel(similarResultsIcon), BorderLayout.CENTER);
        indentablePanel.add(component, BorderLayout.CENTER);
        return indentablePanel;
    }

    private void populateFrom(VisualSearchResult vsr) {
        fromWidget.setPeople(vsr.getSources());
    }

    private void populateHeading(final RowDisplayResult result, BasicDownloadState downloadState, boolean isMouseOver) {
        int width = heading.getVisibleRect().width;
        //Width is zero the first time editorpane is rendered - use a wide default (roughly width of left column)
        width = width == 0 ? LEFT_COLUMN_WIDTH : width;
        //Make the visible rect seem a little smaller than usual to trigger a more hungry truncation
        //otherwise the JEditorPane word-wrapping logic kicks in and the edge word just disappears
        final int fudgeFactorPixelWidth = width - 10;
        SearchHeading searchHeading = new SearchHeading() {
            @Override
            public String getText() {
                String headingText = result.getHeading();
                String truncatedHeading = truncator.truncateHeading(headingText, fudgeFactorPixelWidth, headingFontWidthResolver);
                handleHeadingTooltip(headingText, truncatedHeading);
                return truncatedHeading;
            }

            /**
             * Sets a tooltip for the heading field only if the text has been truncated. 
             */
            private void handleHeadingTooltip(String headingText, String truncatedHeading) {
                String tooltipText = HTML + headingText + CLOSING_HTML_TAG;
                heading.setToolTipText(tooltipText);
                editorComponent.setToolTipText(tooltipText);
            }

            @Override
            public String getText(String adjoiningFragment) {
                int adjoiningTextPixelWidth = headingFontWidthResolver.getPixelWidth(adjoiningFragment);
                String headingText = result.getHeading();
                String truncatedHeading = truncator.truncateHeading(headingText, 
                        fudgeFactorPixelWidth - adjoiningTextPixelWidth, headingFontWidthResolver);
                handleHeadingTooltip(headingText, truncatedHeading);
                return truncatedHeading;
            }
        };

        this.heading.setText(headingBuilder.getHeadingDocument(searchHeading, downloadState, result.isSpam()));
        this.downloadSourceCount.setText(Integer.toString(vsr.getSources().size()));
    }
    
    private class HeadingFontWidthResolver implements FontWidthResolver {
        private static final String EMPTY_STRING = "";
        //finds <b>foo</b> or <a href="#foo"> or {1} patterns
        //**NOTE** This does not account for all HTML sanitizing, just for HTML
        //**NOTE** that would have been added by search result display code
        private final Pattern findHTMLTagsOrReplacementTokens = Pattern.compile("([<][/]?[\\w =\"#]*[>])|([{][\\d]*[}])");
        
        @Override
        public int getPixelWidth(String text) {
            HTMLEditorKit editorKit = (HTMLEditorKit) heading.getEditorKit();
            StyleSheet css = editorKit.getStyleSheet();
            FontMetrics fontMetrics = css.getFontMetrics(headingFont);
            text = findHTMLTagsOrReplacementTokens.matcher(text).replaceAll(EMPTY_STRING);
            return fontMetrics.stringWidth(text);
        }
    }
    
    private void populateOther(RowDisplayResult result) {
        metadataLabel.setText("");
        RowDisplayConfig config = result.getConfig();
        if (config != HeadingSubHeadingAndMetadata && config != RowDisplayConfig.HeadingAndMetadata) { 
            return;
        }
        
        PropertyMatch pm = result.getMetadata();
        
        if (pm != null) {
            String html = pm.getHighlightedValue();
            // Insert the following: the key, a colon and a space after the html start tag, then the closing tags.
            html = html.replace(HTML, "").replace(CLOSING_HTML_TAG, "");
            html = HTML + pm.getKey() + ":" + html + CLOSING_HTML_TAG;
            metadataLabel.setText(html);
        }
    }

    private void populatePanel(VisualSearchResult vsr, int column) {
        if (vsr == null) return;
        
        if (column == 0) {
            if (vsr.getSimilarityParent() != null) {
                indentablePanel.add(similarResultIndentation, BorderLayout.WEST);
                similarResultIndentation.setBackground(similarResultsBackgroundColor);
            } else {
                indentablePanel.remove(similarResultIndentation);
            }
            
            itemIconLabel.setIcon(getIcon(vsr));
            itemIconLabel.setCursor(getIconCursor(vsr));

            RowDisplayResult result = rowHeightRule.getDisplayResult(vsr, searchText);
            
            organizeSearchResultLayout(result.getConfig());
            
            populateHeading(result, vsr.getDownloadState(), false);
            populateSubheading(result);
            populateOther(result);
            
        } else if (column == 1) {
            similarButton.setVisible(getSimilarResultsCount() > 0);
            populateFrom(vsr);
        }
    }

    private Icon getIcon(VisualSearchResult vsr) {
        if (vsr.isSpam()) {
            return spamIcon;
        } 
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
            return downloadingIcon;
        case DOWNLOADED:
        case LIBRARY:
            return libraryIcon;
        }
        return categoryIconManager.getIcon(vsr, iconManager);
    }

    private void organizeSearchResultLayout(RowDisplayConfig config) {
        String labelPadding = OSUtils.isMacOSX() ? "" : "pad -6 4 0 0,";
        switch(config) {
        case HeadingOnly:
            searchResultTextPanel.remove(subheadingLabel);
            searchResultTextPanel.remove(metadataLabel);
            break;
        case HeadingAndSubheading:
            searchResultTextPanel.add(subheadingLabel, labelPadding + "cell 0 1");
            searchResultTextPanel.remove(metadataLabel);
            break;
        case HeadingAndMetadata:
            searchResultTextPanel.remove(subheadingLabel);
            searchResultTextPanel.add(metadataLabel, labelPadding + "cell 0 1");
            break;
        case HeadingSubHeadingAndMetadata:
            searchResultTextPanel.add(subheadingLabel, (OSUtils.isMacOSX() ? "" : "pad -3 4 0 0,") + "cell 0 1, wrap");
            searchResultTextPanel.add(metadataLabel, (OSUtils.isMacOSX() ? "" : "pad 0 4 0 0, gapbottom 3,") + "cell 0 2");
        }
    }

    /**
     * Returns a value to indicate whether the subheading was decorated to highlight
     * parts of the content that match search terms
     */
    private void populateSubheading(RowDisplayResult result) {
        subheadingLabel.setText(result.getSubheading());
    }
}