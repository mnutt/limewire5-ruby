package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.options.TabItemListener;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.util.EnabledListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Main content panel for the Advanced Tools window.  The window is displayed
 * by calling the <code>display(TabId)</code> method. 
 */
@Singleton
public class AdvancedToolsPanel extends JPanel {
    /** Defines the tab identifiers for the window. */
    public enum TabId {
        CONNECTIONS(I18n.tr("Connections"), I18n.tr("View connections to other P2P clients")), 
        CONSOLE(I18n.tr("Console"), I18n.tr("View console messages")),
        MOJITO(I18n.tr("Mojito"), I18n.tr("View incoming and outgoing DHT messages"));
        
        private final String name;
        private final String tooltip;
        
        TabId(String name, String tooltip) {
            this.name = name;
            this.tooltip = tooltip;
        }
        
        public String tooltip() {
            return tooltip;
        }
        
        public String toString() {
            return name;
        }
    }
    
    /** Identifier for Advanced Tools window. */
    private static final String WINDOW_TITLE = I18n.tr("Advanced Tools");

    @Resource
    private Color backgroundColor;
    @Resource
    private Icon connectionsIcon;
    @Resource
    private Icon consoleIcon;
    @Resource
    private Icon mojitoIcon;
    @Resource
    private Color tabTopColor;
    @Resource
    private Color tabBottomColor;
    @Resource
    private Color tabFontColor;
    @Resource
    private Font tabFont;

    /** Action to select next tab. */
    private Action nextTabAction = new NextTabAction();
    /** Action to select previous tab. */
    private Action prevTabAction = new PrevTabAction();
    /** Window listener for main frame. */
    private WindowListener mainFrameListener = new MainFrameListener();
    /** Currently selected tab item. */
    private AdvancedTabItem selectedItem;
    /** Map containing tab items. */
    private Map<TabId, AdvancedTabItem> tabItemMap = new EnumMap<TabId, AdvancedTabItem>(TabId.class);
    /** Map containing tab panels created. */
    private Map<TabId, TabPanel> tabPanelMap = new EnumMap<TabId, TabPanel>(TabId.class);
    /** Window used to display the panel. */
    private Window window;
    
    private JXPanel headerPanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    /**
     * Constructs a window panel for the Advanced Tools window that uses the 
     * injected Provider instances to create the tab content panels.
     */
    @Inject
    public AdvancedToolsPanel(BarPainterFactory barPainterFactory, 
        Provider<ConnectionsPanel> connectionsPanel, 
        Provider<ConsolePanel> consolePanel,
        Provider<MojitoPanel> mojitoPanel) {

        // Inject annotated resource values.
        GuiUtils.assignResources(this);

        // Initialize components.
        initComponents(barPainterFactory);
        
        // Add tabs to dialog.
        addTab(TabId.CONNECTIONS, connectionsIcon, connectionsPanel);
        addTab(TabId.CONSOLE, consoleIcon, consolePanel);
        addTab(TabId.MOJITO, mojitoIcon, mojitoPanel);
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents(BarPainterFactory barPainterFactory) {
        setLayout(new BorderLayout());

        // Create header panel to hold tab buttons.
        headerPanel = new JXPanel();
        headerPanel.setBackgroundPainter(barPainterFactory.createTopBarPainter());
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0",
                "",                  // col constraints
                "align top,fill"));  // row constraints
        
        // Create panel to hold tab content panels.
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setBackground(backgroundColor);
        cardPanel.setLayout(cardLayout);
        
        add(headerPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Adds a tab to the dialog using the specified tab identifier, icon, and
     * panel provider. 
     */
    private void addTab(TabId tabId, Icon icon, Provider<? extends TabPanel> provider) {
        // Create tab item and add to map.
        AdvancedTabItem tabItem = new AdvancedTabItem(tabId, icon, provider);
        tabItemMap.put(tabId, tabItem);
        
        // Add button to header.
        headerPanel.add(createButton(tabItem));
    }

    /**
     * Creates a tab button for the specified tab item.
     */
    private JButton createButton(AdvancedTabItem tabItem) {
        // Create the tab button.
        TabButton button = new TabButton(new TabAction(tabItem, tabItem.getIcon()));
        
        // Set gradient colors.
        button.setGradients(tabTopColor, tabBottomColor);
        button.setForeground(tabFontColor);
        button.setFont(tabFont);
        
        // Add inputs and action to select previous tab.
        button.getActionMap().put(PrevTabAction.KEY, this.prevTabAction);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), PrevTabAction.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), PrevTabAction.KEY);
        
        // Add inputs and action to select next tab.
        button.getActionMap().put(NextTabAction.KEY, this.nextTabAction);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NextTabAction.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), NextTabAction.KEY);

        return button;
    }
    
    /**
     * Displays this panel in a modeless window.
     */
    public void display() {
        if (this.window == null) {
            // Create modeless window.
            JFrame frame = new LimeJFrame(WINDOW_TITLE);

            // Set window properties. 
            frame.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            frame.setResizable(true);

            // Set preferred size based on main frame.
            Dimension mainSize = GuiUtils.getMainFrame().getSize();
            Dimension prefSize = new Dimension(
                    Math.max(mainSize.width - 60, 800),
                    Math.max(mainSize.height - 60, 600));
            frame.setPreferredSize(prefSize);
            
            // Add listener to handle system menu close action.
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    disposeWindow();
                }
            });
            
            // Add listener to handle parent window events.
            GuiUtils.getMainFrame().addWindowListener(this.mainFrameListener);

            // Set content pane and validate.
            frame.setContentPane(this);
            frame.pack();

            // Set window position centered on main frame.
            frame.setLocationRelativeTo(GuiUtils.getMainFrame());
            
            // Save window reference.
            this.window = frame;

            // Select first enabled tab.
            select(findNextEnabledTab(-1, true));
            
            // Start tab panels.
            for (TabId tabId : TabId.values()) {
                TabPanel tabPanel = tabItemMap.get(tabId).getTabPanel();
                if (tabPanel != null) {
                    tabPanel.start();
                }
            }
        }

        // Display window.
        this.window.setVisible(true);
        this.window.toFront();
    }
    
    /**
     * Closes the window that is displaying this panel.
     */
    public void disposeWindow() {
        if (this.window != null) {
            // Stop tab panels.
            for (TabId tabId : TabId.values()) {
                TabPanel tabPanel = tabItemMap.get(tabId).getTabPanel();
                if (tabPanel != null) {
                    tabPanel.stop();
                }
            }
            
            // Remove window listener from main GUI frame.
            GuiUtils.getMainFrame().removeWindowListener(this.mainFrameListener);
            
            // Dispose of this window and clear reference.
            this.window.dispose();
            this.window = null;
        }
    }
    
    /**
     * Minimizes the window that is displaying this panel.
     * @param visible true if minimized window remains visible in taskbar
     */
    public void minimizeWindow(boolean visible) {
        if (this.window instanceof Frame) {
            ((Frame) this.window).setExtendedState(Frame.ICONIFIED);
            this.window.setVisible(visible);
        }
    }
    
    /**
     * Restores the window that is displaying this panel.
     */
    public void restoreWindow() {
        if (this.window instanceof Frame) {
            ((Frame) this.window).setExtendedState(Frame.NORMAL);
            this.window.setVisible(true);
        }
    }
    
    /**
     * Returns the TabId of the next enabled tab after the specified tab index.
     * The <code>forward</code> argument specifies direction: true for next
     * tab, false for previous tab.  If no other enabled tabs are found, then
     * null is returned.
     */
    private TabId findNextEnabledTab(int startIndex, boolean forward) {
        // Get array of TabId values.
        TabId[] tabIds = TabId.values();

        // Normalize start index.
        startIndex = (startIndex + tabIds.length) % tabIds.length;
        
        // Get index of next/previous tab.
        int nextTab = (startIndex + (forward ? 1 : -1) + tabIds.length) % tabIds.length;
        boolean enabled = tabItemMap.get(tabIds[nextTab]).isEnabled();
        
        // Find next/previous enabled tab.  If no enabled tabs are found, 
        // then return the start index.
        while (!enabled && (nextTab != startIndex)) {
            nextTab = (nextTab + (forward ? 1 : -1) + tabIds.length) % tabIds.length;
            enabled = tabItemMap.get(tabIds[nextTab]).isEnabled();
        }
        
        // Return enabled tab, or null if none found.
        return (nextTab != startIndex) ? tabIds[nextTab] : null;
    }
    
    /**
     * Selects the tab item with the specified identifier.
     * @param tabId the identifier of the tab
     */
    private void select(TabId tabId) {
        // Skip if identifier is null.
        if (tabId == null) {
            return;
        }
        
        // De-select current item.
        if (selectedItem != null) {
            selectedItem.fireSelected(false);
        }
        
        // Get new selected item.
        selectedItem = tabItemMap.get(tabId);

        // Get tab panel - lazily create if necessary.
        TabPanel tabPanel = tabPanelMap.get(tabId);
        if (tabPanel == null) {
            tabPanel = selectedItem.getTabPanel();
            tabPanel.setBackground(backgroundColor);
            tabPanelMap.put(tabId, tabPanel);
            cardPanel.add(tabPanel, tabId.toString());
        }
        
        // Fire event to select tab.
        selectedItem.fireSelected(true);

        // Display selected tab panel.
        cardLayout.show(cardPanel, tabId.toString());
    }

    /**
     * Selects the next enabled tab item.
     */
    private void selectNext() {
        // Get index for selected tab item.
        int index = selectedItem.getTabId().ordinal();

        // Select next enabled tab.
        select(findNextEnabledTab(index, true));
    }
    
    /**
     * Selects the previous enabled tab item.
     */
    private void selectPrev() {
        // Get index for selected tab item.
        int index = selectedItem.getTabId().ordinal();

        // Select previous enabled tab.
        select(findNextEnabledTab(index, false));
    }
    
    /**
     * A TabItem for the Advanced Tools dialog. 
     */
    private class AdvancedTabItem extends AbstractTabItem {

        private final TabId tabId;
        private final Icon icon;
        private final Provider<? extends TabPanel> provider;
        private TabPanel tabPanel;

        /**
         * Constructs a tab item using the specified tab identifier, icon, and
         * panel provider.
         */
        public AdvancedTabItem(TabId tabId, Icon icon, 
                Provider<? extends TabPanel> provider) {
            this.tabId = tabId;
            this.icon = icon;
            this.provider = provider;
        }

        @Override
        public String getId() {
            return tabId.toString();
        }

        @Override
        public void select() {
            AdvancedToolsPanel.this.select(tabId);
        }

        public boolean isEnabled() {
            return getTabPanel().isTabEnabled();
        }
        
        public Icon getIcon() {
            return icon;
        }
        
        public TabId getTabId() {
            return tabId;
        }

        public TabPanel getTabPanel() {
            if (tabPanel == null) {
                tabPanel = this.provider.get();
            }
            return tabPanel;
        }
    }

    /**
     * An Action associated with a tab button.  This updates the "selected" 
     * value when its associated tab item is selected or de-selected.
     */
    private class TabAction extends AbstractAction {
        
        private final TabId tabId;
        
        /**
         * Constructs a TabAction with the specified <code>AdvancedTabItem
         * </code> and icon.  The Action.NAME value is set to the translated 
         * string for the tab.
         */
        public TabAction(AdvancedTabItem tabItem, Icon icon) {
            super(tabItem.getTabId().toString(), icon);
            
            // Store tab identifier and action command.
            tabId = tabItem.getTabId();
            putValue(ACTION_COMMAND_KEY, tabId.toString());
            putValue(SHORT_DESCRIPTION, tabId.tooltip());

            // Install listener to handle tab item selection. 
            tabItem.addTabItemListener(new TabItemListener() {
                @Override
                public void itemSelected(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                }
            });
            
            // Install listener to handle tab panel enabled.
            TabPanel tabPanel = tabItem.getTabPanel();
            tabPanel.addEnabledListener(new EnabledListener() {
                @Override
                public void enabledChanged(boolean enabled) {
                    setEnabled(enabled);
                }
            });
            
            // Initialize enabled state.
            setEnabled(tabPanel.isTabEnabled());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            select(tabId);
        }
    }

    /**
     * An Action that handles events to select the next tab.
     */
    private class NextTabAction extends AbstractAction {
        final static String KEY = "NEXT";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectNext();
        }
    }
    
    /**
     * An Action that handles events to select the previous tab.
     */
    private class PrevTabAction extends AbstractAction {
        final static String KEY = "PREV";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectPrev();
        }
    }
    
    /**
     * A listener that handles window events on the main GUI frame.  This is
     * used to minimize or restore the Advanced Tools window when the main GUI
     * frame is minimized or restored. 
     */
    private class MainFrameListener extends WindowAdapter {
        
        public void windowClosed(WindowEvent e) {
            disposeWindow();
        }
        
        public void windowDeiconified(WindowEvent e) {
            restoreWindow();
        }
        
        public void windowIconified(WindowEvent e) {
            minimizeWindow(e.getWindow().isVisible());
        }
    }
}
