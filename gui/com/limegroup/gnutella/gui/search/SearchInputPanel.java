package com.limegroup.gnutella.gui.search;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import org.limewire.core.settings.FilterSettings;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.gui.AutoCompleteTextField;
import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.ClearableAutoCompleteTextField;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.KeyProcessingTextField;
import com.limegroup.gnutella.gui.MySharedFilesButton;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.gui.xml.InputPanel;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 * Inner panel that switches between the various kinds of
 * searching.
 */
class SearchInputPanel extends JPanel {
    
    /**
     * The current search label in what's new.
     */
    private final JLabel WHATSNEW_SEARCH_LABEL = new JLabel();

    /**
     * The current search label in normal search.
     */
    private final JLabel SEARCH_TYPE_LABEL = new JLabel();
    
    /**
     * The sole input text field that is at the top of all searches.
     */
    private final SearchField SEARCH_FIELD = new SearchField(14);
    
    /**
     * The text area that contains the information about direct-connecting
     * to this host.
     */
    private final JTextArea IP_TEXT = new JTextArea();
    
    /**
     * The input field for browse-host searches.
     */
    private final AutoCompleteTextField BROWSE_HOST_FIELD =
        new ClearableAutoCompleteTextField();
    
    /**
     * The What's New search button.
     */
    private final JButton WHATSNEW = new JButton(
            I18n.tr("What\'s New"));
    
    /**
     * The JTabbedPane that switches between types of searches.
     */
    private final JTabbedPane PANE = new JTabbedPane(JTabbedPane.BOTTOM);
        
    /**
     * The CardLayout that switches between the detailed
     * search input information for each meta-type.
     */
    private final CardLayout META_CARDS = new CardLayout();
    
    /**
     * The panel that the META_CARDS layout uses to layout
     * the detailed search input fields.
     */
    private final JPanel META_PANEL = new JPanel(META_CARDS);
    
    /**
     * The name to use for the default panel that has no meta-data.
     */
    private static final String DEFAULT_PANEL_KEY = "defaultPanel";
    
    /**
     * The box that holds the schemas for searching.
     */
    private final SchemaBox SCHEMA_BOX = new SchemaBox();
    
    /**
     * The ditherer to use for the tab backgrounds.
     */
    private final Ditherer DITHERER =
            new Ditherer(62,
                        ThemeFileHandler.SEARCH_PANEL_BG_1.getValue(), 
                        ThemeFileHandler.SEARCH_PANEL_BG_2.getValue()
                        );
                    
	private JPanel searchEntry;
    
    /**
     * The listener for new searches.
     */
    private final ActionListener SEARCH_LISTENER = new SearchListener();
    
	/**
	 * Holds the keys of the already created input panels.
	 */
	private Set<String> inputPanelKeys = null;
    
    /**
     * A HashMap for each input panel's preferred dimension 
     * where the key is the <tt>NameMediaType</tt> of the panel
     */
    private Map<NamedMediaType, Dimension> inputPanelDimensions = new HashMap<NamedMediaType, Dimension>();
    
    private final NetworkManager networkManager;
    private final NetworkInstanceUtils networkInstanceUtils;
    

    @InspectionPoint("whatsNewByType")
    private static final InspectionHistogram<String> whatsNewSearches = new InspectionHistogram<String>();
    
    @InspectionPoint("searchByType")
    private static final InspectionHistogram<String> regularSearches = new InspectionHistogram<String>();
    
    @InspectionPoint("numberOfXMLFields")
    private static final InspectionHistogram<Integer> numberOfXmlFields = new InspectionHistogram<Integer>();
        
    SearchInputPanel(NetworkManager networkManager, NetworkInstanceUtils networkInstanceUtils) {
        super(new BorderLayout(0, 5));
        
        this.networkManager = networkManager; 
        this.networkInstanceUtils = networkInstanceUtils;

        final ActionListener schemaListener = new SchemaListener();
        SCHEMA_BOX.addSelectionListener(schemaListener);
        add(SCHEMA_BOX, BorderLayout.NORTH);

        searchEntry = createSearchEntryPanel();
		
        JPanel whatsnew = createWhatIsNewPanel();
        JPanel browseHost = createBrowseHostPanel();
        panelize(searchEntry);
        panelize(whatsnew);
        panelize(browseHost);
        PANE.add(I18n.tr("Keyword"),
                 searchEntry);
        PANE.add(I18n.tr("What\'s New"),
                 whatsnew);
        PANE.add(I18n.tr("Direct Connect"),
                 browseHost);
                 
        PANE.setRequestFocusEnabled(false);
        PANE.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                requestSearchFocusImmediately();
            }
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
        });         

        if(!ThemeSettings.isNativeTheme()) {
            PANE.setBorder(
              new LineBorder(ThemeFileHandler.SEARCH_GRID_COLOR.getValue()) {
                @Override
                public void paintBorder(Component c, Graphics g,
                                        int x, int y, int width, int height) {
                    try {
                        Component sel = PANE.getSelectedComponent();
                        if(sel != null)
                            height = sel.getBounds().height + 4;                    
                    } catch(ArrayIndexOutOfBoundsException aioobe) {}
                    super.paintBorder(c, g, x, y, width, height);
                }
            });
        }
        add(PANE, BorderLayout.CENTER);

        JPanel viewSharedFilesPanel = new BoxPanel(BoxPanel.X_AXIS);
        viewSharedFilesPanel.add(new JLabel(GUIMediator.getThemeImage("shared_folder")));
        viewSharedFilesPanel.add(Box.createHorizontalStrut(5));
        viewSharedFilesPanel.add(new MySharedFilesButton());
        viewSharedFilesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 0));
        add(viewSharedFilesPanel, BorderLayout.SOUTH);
        
        WHATSNEW_SEARCH_LABEL.setFont(UIManager.getFont("Table.font.bold"));
        Font bold = UIManager.getFont("Table.font.bold");
        Font bolder =
            new Font(bold.getName(), bold.getStyle(), bold.getSize() + 5);
        SEARCH_TYPE_LABEL.setFont(bolder);
        SEARCH_TYPE_LABEL.setPreferredSize(new Dimension(100, 20));
        schemaListener.actionPerformed(null);
    }
    
    /**
     * Gets the KeyProcessingTextField that key events can be forwarded to.
     */
    KeyProcessingTextField getForwardingSearchField() {
        if(isNormalSearchType()) {
            if(SCHEMA_BOX.getSelectedSchema() != null) {
                return getInputPanel().getFirstTextField();
            }
            return SEARCH_FIELD;
        }
		if(isBrowseHostSearchType())
            return BROWSE_HOST_FIELD;
		return null;
    }
    
    /**
     * Determines if a key event can be forwarded to the search.
     */
    boolean isKeyEventForwardable() {
        return isNormalSearchType() ||
               isBrowseHostSearchType();
    }
    
    /**
     * Determines if browse-host is selected.
     */
    boolean isBrowseHostSearchType() {
        return PANE.getSelectedIndex() == 2;
    }
    
    /**
     * Determines if what is new is selected.
     */
    boolean isWhatIsNewSearchType() {
        return PANE.getSelectedIndex() == 1;
    }
    
    /**
     * Determines if keyword is selected.
     */
    boolean isNormalSearchType() {
        return PANE.getSelectedIndex() == 0;
    }
    
    /**
     * Notification that the addr has changed.
     */
    void addressChanged() {
        updateIpText();
        invalidate();
        revalidate();
    }
    
    void requestSearchFocusImmediately() {
        if(isNormalSearchType()) {
            if(SCHEMA_BOX.getSelectedSchema() != null) {
                getInputPanel().requestFirstFocus();
            } else {
                SEARCH_FIELD.requestFocus();
            }
        } else if(isWhatIsNewSearchType()) {
            WHATSNEW.requestFocus();
        } else if(isBrowseHostSearchType()) {
            BROWSE_HOST_FIELD.requestFocus();
        }
    }
    
    void requestSearchFocus() {
        // Workaround for bug manifested on Java 1.3 where FocusEvents
        // are improperly posted, causing BasicTabbedPaneUI to throw an
        // ArrayIndexOutOfBoundsException.
        // See:
        // http://developer.java.sun.com/developer/bugParade/bugs/4523606.html
        // http://developer.java.sun.com/developer/bugParade/bugs/4379600.html
        // http://developer.java.sun.com/developer/bugParade/bugs/4128120.html
        // for related problems.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                requestSearchFocusImmediately();
            }
        });
    }
    
    /**
     * Sets all components in this component to be not opaque
     * and sets the correct background panel.
     */
    private void panelize(JComponent c) {
        GUIUtils.setOpaque(false, c);
        if(!ThemeSettings.isNativeTheme())
            c.setOpaque(true);
        c.setBackground(ThemeFileHandler.SEARCH_PANEL_BG_2.getValue());
        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
    }
    
    /**
     * Creates the 'Direct Connect' browse host input panel.
     */
    private JPanel createBrowseHostPanel() {
        BROWSE_HOST_FIELD.addActionListener(SEARCH_LISTENER);
        
        JPanel panel = new DitherPanel(DITHERER);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        
        JLabel label = new JLabel(I18n.tr("Got Friends?"));
        label.setFont(UIManager.getFont("Table.font.bold"));
        panel.add(label, c);
        
        JTextArea text1 = new JTextArea(I18n.tr("Enter the IP address and port number (i.e. ip:port) of a friend you\'d like to connect to, click \'Direct Connect\', and LimeWire will try to browse that user's shared files."));
        text1.setLineWrap(true);
        text1.setWrapStyleWord(true);
        text1.setColumns(15);
        text1.setEditable(false);
        text1.setFont(UIManager.getFont("Table.font"));
        text1.setForeground(label.getForeground());
        c.insets = new Insets(15, 0, 0, 0);
        panel.add(text1, c);
                
        updateIpText();
        IP_TEXT.setLineWrap(true);
        IP_TEXT.setWrapStyleWord(true);
        IP_TEXT.setColumns(15);
        IP_TEXT.setEditable(false);
        IP_TEXT.setFont(UIManager.getFont("Table.font"));
        IP_TEXT.setForeground(label.getForeground());
        panel.add(IP_TEXT, c);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(BROWSE_HOST_FIELD, c);
        
        JButton search = new JButton(I18n.tr("Direct Connect"));
        search.addActionListener(SEARCH_LISTENER);
        c.fill = GridBagConstraints.NONE;
        panel.add(search, c);
        
        c.weighty = 1;
        panel.add(Box.createVerticalGlue(), c);
        
        return panel;
    }        
        
    private void updateIpText() {
        if(networkManager.acceptedIncomingConnection() &&
           !networkInstanceUtils.isPrivate()) {
            IP_TEXT.setText(
        			I18n.tr("When your friends want to connect to you, they should enter")+" \""+
					NetworkUtils.ip2string(networkManager.getAddress())+":"
					+networkManager.getPort()+"\""
			       );
        } else {
            IP_TEXT.setText(I18n.tr("Your computer is behind a firewall or router and cannot receive direct connections."));
        }
    }
    
    /**
     * Creates the What's New input panel.
     */
    private JPanel createWhatIsNewPanel() {
        JPanel panel = new DitherPanel(DITHERER);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        
        JLabel label = new JLabel(I18n.tr("Don\'t Know"));
        label.setFont(UIManager.getFont("Table.font.bold"));
        panel.add(label, c);
        
        JLabel label2 = new JLabel(I18n.tr("What To Look For?"));        
        label2.setFont(UIManager.getFont("Table.font.bold"));
        panel.add(label2, c);
        
        JTextArea text = new JTextArea(I18n.tr("A \"What\'s New\" search will search for files that have been recently added to the network."));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setColumns(15);
        text.setEditable(false);
        text.setFont(UIManager.getFont("Table.font"));
        text.setForeground(label.getForeground());
        c.insets = new Insets(15, 0, 30, 0);
        panel.add(text, c);
        
        JTextArea type = new JTextArea(
            I18n.tr("Current Search:") + 
            "  ");
        type.setFont(UIManager.getFont("Table.font"));
        type.setEditable(false);
        type.setForeground(label.getForeground());
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(type, c);
        
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(WHATSNEW_SEARCH_LABEL, c);
        
        WHATSNEW.addActionListener(SEARCH_LISTENER);
        c.insets = new Insets(5, 0, 30, 0);
        c.anchor = GridBagConstraints.CENTER;
        panel.add(WHATSNEW, c);

        final JCheckBox hideAdult = new JCheckBox(
            I18n.tr("Hide Adult Content"),
            FilterSettings.FILTER_WHATS_NEW_ADULT.getValue());
        hideAdult.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                FilterSettings.FILTER_WHATS_NEW_ADULT.
                    setValue(hideAdult.isSelected());
            }
        });
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(hideAdult, c);
        hideAdult.setVisible(!FilterSettings.FILTER_ADULT.getValue());
        
        // Add a blank item with a weighty that'll push the rest to the top.
        c.weighty = 1;
        panel.add(Box.createVerticalGlue(), c);

        return panel;
    }

    private JPanel createSearchEntryPanel() {
        SEARCH_FIELD.addActionListener(SEARCH_LISTENER);

        // add the default search input panel to the meta cards
        META_PANEL.add(createDefaultSearchPanel(), DEFAULT_PANEL_KEY);

		// other mediatype panels are added lazily on demand

        JPanel search = new DitherPanel(DITHERER);
        search.setLayout(new BoxLayout(search, BoxLayout.Y_AXIS));
        search.add(GUIUtils.left(SEARCH_TYPE_LABEL));
        search.add(Box.createVerticalStrut(5));
        search.add(META_PANEL);
        return search;
    }
    
	private void createInputPanelForNamedMediaType(NamedMediaType nmt) {
        String name = nmt.getName();
        LimeXMLSchema schema = nmt.getSchema();
        // If a schema exists, add it as a possible type.
        if (schema == null) {
            throw new NullPointerException("named mediatype has no schema");
        }
		
		InputPanel panel = new InputPanel(schema, SEARCH_LISTENER, 
				SEARCH_FIELD.getDocument(),
				SEARCH_FIELD.getUndoManager());
		panel.addMoreOptionsListener(new MoreOptionsListener());
		JScrollPane pane = new JScrollPane(panel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cleanupPaneActions(pane.getActionMap());
		pane.setOpaque(false);
		pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBorder(null);
		JPanel outerPanel = new JPanel(new BorderLayout());
		outerPanel.add(pane,"Center");
		outerPanel.add(createSearchButtonPanel(),"South");
		int paneWidth = (int)pane.getPreferredSize().getWidth();
		int paneHeight= (int)pane.getPreferredSize().getHeight();
        Dimension dim = new Dimension(paneWidth+70,paneHeight+30);
        outerPanel.setMaximumSize(dim);
	    inputPanelDimensions.put(nmt,dim);
        JPanel holdingPanel = new JPanel();
	    holdingPanel.setLayout(new BoxLayout(holdingPanel,BoxLayout.Y_AXIS));
	    holdingPanel.add(outerPanel);
		getInputPanelKeys().add(name);
		META_PANEL.add(holdingPanel, name);
		panelize(searchEntry);
	}
    
    private void cleanupPaneActions(ActionMap map) {
        if(map == null)
            return;
        
        Object[] keys = map.allKeys();
        for(int i = 0; i < keys.length; i++) {
            Action action = map.get(keys[i]);
            if(action == null)
                continue;
            Object o = action.getValue(Action.NAME);
            if(!(o instanceof String))
                return;
            String name = (String)o;
            if(name.equals("scrollHome") ||
               name.equals("scrollEnd") ||
               name.equals("scrollLeft") ||
               name.equals("scrollRight") ||
               name.equals("unitScrollLeft") ||
               name.equals("unitScrollRight")) {
                map.remove(keys[i]);
                if(map.get(keys[i]) != null)
                    cleanupPaneActions(map.getParent());
            }
        }
    }
        
	private Set<String> getInputPanelKeys() {
		if (inputPanelKeys == null)
			inputPanelKeys = new HashSet<String>();
		return inputPanelKeys;
    }
    
    /**
     * Creates the default search input of:
     *    Filename
     *    [   input box  ]
     */
    private JPanel createDefaultSearchPanel() {
        
        JPanel label = createLabel(
			I18n.tr("Filename"));
        JPanel fullPanel = new BoxPanel(BoxPanel.Y_AXIS);
        fullPanel.add(label);
        fullPanel.add(Box.createVerticalStrut(3));
        fullPanel.add(GUIUtils.left(SEARCH_FIELD));
        fullPanel.add(Box.createVerticalStrut(5));
        fullPanel.add(createSearchButtonPanel());

        return GUIUtils.left(fullPanel);
    }
    
    private JPanel createLabel(String text) {
        JPanel labelPanel = new BoxPanel(BoxPanel.X_AXIS);
        labelPanel.setOpaque(false);
        labelPanel.add(new JLabel(text));
        labelPanel.add(Box.createHorizontalGlue());		
        return labelPanel;
    }    

    /**
     * Creates the search button & inserts it in a panel.
     */
    private JPanel createSearchButtonPanel() {
        JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton searchButton = new JButton(
            I18n.tr("Search"));        
        searchButton.setToolTipText(
            I18n.tr("Search the Network for the Given Words"));

        searchButton.addActionListener(SEARCH_LISTENER);
      
        b.add(Box.createHorizontalGlue());
        b.add(searchButton);
        return b;
    }
    
    /**
     * Gets the visible component in META_PANEL.
     */
    private JComponent getVisibleComponent() {
        for(int i = 0; i < META_PANEL.getComponentCount(); i++) {
            Component current = META_PANEL.getComponent(i);
            if(current.isVisible())
                return (JComponent)current;
        }
        return null;
    }
    
    /**
     * Gets the visible scrollpane.
     */
    private JScrollPane getVisibleScrollPane() {
        JComponent parent = (JComponent)getVisibleComponent().getComponent(0);
        	for(int i = 0; i < parent.getComponentCount(); i++) {
            Component current = parent.getComponent(i);
            if(current.isVisible() && current instanceof JScrollPane)
                return (JScrollPane)current;
        }
        return null;
    }
    
    /**
     * Retrieves the InputPanel that is currently visible.
     */
    private InputPanel getInputPanel() {
        JScrollPane pane = getVisibleScrollPane();
        if(pane == null)
            return null;
        else
            return (InputPanel)pane.getViewport().getView();
    }
    
	/**
	 * Listener for selecting a new schema.
	 */
	private class SchemaListener implements ActionListener {
	    public void actionPerformed(ActionEvent event) {
	        if(SCHEMA_BOX.getSelectedSchema() != null) {
				String key = SCHEMA_BOX.getSelectedItem();
				if (!getInputPanelKeys().contains(key))
					createInputPanelForNamedMediaType(SCHEMA_BOX.getSelectedMedia());
	            META_CARDS.show(META_PANEL, key);
            } else {
                META_CARDS.show(META_PANEL, DEFAULT_PANEL_KEY);
            }
            WHATSNEW_SEARCH_LABEL.setText(SCHEMA_BOX.getSelectedItem());
            WHATSNEW_SEARCH_LABEL.setPreferredSize(
                new Dimension(GUIUtils.width(WHATSNEW_SEARCH_LABEL), 20));
            SEARCH_TYPE_LABEL.setText(SCHEMA_BOX.getSelectedItem());
            requestSearchFocus();
        }
    }
    
    /**
     * Listener for starting a new search.
     */
    private class SearchListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            MediaType mt = SCHEMA_BOX.getSelectedMediaType();
            InputPanel panel = null;
            final SearchInformation info;
            
            if(isWhatIsNewSearchType()) {
                info = SearchInformation.createWhatsNewSearch(
                    SCHEMA_BOX.getSelectedItem(), mt);
                whatsNewSearches.count(mt != null ? mt.getSchema() : "all");
            } else if(isNormalSearchType()) {
                String query = null;
                String xml = null;
                String title = null;
                if(SCHEMA_BOX.getSelectedSchema() != null) {
                    panel = getInputPanel();
                    query = panel.getStandardQuery();
                    xml = panel.getInput(true);
                    if (xml == null && mt != null) {
                        numberOfXmlFields.count(0);
                    } else {
                        numberOfXmlFields.count(panel.getNumberOfFieldsWithInput());
                    }
                    title = panel.getTitleForQuery();
                } else {
                    query = SEARCH_FIELD.getText();
                    title = query;
                }
                info = SearchInformation.createTitledKeywordSearch(query, xml, mt, title);
                regularSearches.count(mt != null ? mt.getSchema() : "all");
            } else if(isBrowseHostSearchType()) {
                String user = BROWSE_HOST_FIELD.getText();
                if (!NetworkUtils.isAddress(user)) {
                    GUIMediator.showError(I18n.tr("The address format is incorrect, please use host:port."));
                    return;
                }
                info = SearchInformation.createBrowseHostSearch(user);
                mt = MediaType.getAnyTypeMediaType(); // always any
            } else {
                throw new IllegalStateException("Invalid search: " + e);
            }
            
            // If the search worked, store & clear it.
            if(SearchMediator.triggerSearch(info) != null) {
                if(info.isKeywordSearch()) {
                    // Add the necessary stuff for autocompletion.
                    if(panel != null) {
                        panel.storeInput();
                        panel.clear();
                    } else {
                        SEARCH_FIELD.addToDictionary();
                    }
    
                    // Clear the existing search.
                    SEARCH_FIELD.setText("");
                } else if(info.isBrowseHostSearch()) {
                    BROWSE_HOST_FIELD.addToDictionary();
                    BROWSE_HOST_FIELD.setText("");
                }
            }
        }
    }
    
    /**
     * Listener for 'more options'.
     */
    private class MoreOptionsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JCheckBox box = (JCheckBox)e.getSource();
            JComponent c = (JComponent)getVisibleComponent().getComponent(0);
            JComponent pane = (JComponent)((JComponent)c.getComponent(0)).getComponent(0);
            if(c instanceof JPanel && pane instanceof JViewport) {
            	if(box.isSelected()) {
            		c.setMaximumSize(null);
	            }
            	else {
                    Dimension dim = inputPanelDimensions.get(SCHEMA_BOX.getSelectedMedia());
            		if(dim != null)
            		c.setMaximumSize(dim);
            	}
            }
            invalidate();
            revalidate();
            repaint();
        }
    }
    
}    
