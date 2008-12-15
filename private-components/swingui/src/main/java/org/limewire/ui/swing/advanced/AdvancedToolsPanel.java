package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.options.TabItemListener;
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
        CONNECTIONS(I18n.tr("Connections")), 
        //LOGGING(I18n.tr("Logging")), // removed obsolete feature
        CONSOLE(I18n.tr("Console"));
        
        private final String name;
        
        TabId(String name) {
            this.name = name;
        }
        
        public String toString() {
            return name;
        }
    }
    
    /** Identifier for Advanced Tools window. */
    private static final String WINDOW_TITLE = I18n.tr("Advanced Tools");

    @Resource
    private Color headerColor;
    @Resource
    private Color dividerColor;
    @Resource
    private Color backgroundColor;
    @Resource
    private Icon connectionsIcon;
    @Resource
    private Icon consoleIcon;
    @Resource
    private Color tabTopColor;
    @Resource
    private Color tabBottomColor;

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
    
    private JPanel headerPanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    /**
     * Constructs a window panel for the Advanced Tools window that uses the 
     * injected Provider instances to create the tab content panels.
     */
    @Inject
    public AdvancedToolsPanel(Provider<ConnectionsPanel> connectionsPanel, 
        Provider<ConsolePanel> consolePanel) {

        // Inject annotated resource values.
        GuiUtils.assignResources(this);

        // Initialize components.
        initComponents();
        
        // Add tabs to dialog.
        addTab(TabId.CONNECTIONS, connectionsIcon, connectionsPanel);
        //addTab(TabId.LOGGING, null, loggingPanel);
        addTab(TabId.CONSOLE, consoleIcon, consolePanel);
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(new BorderLayout());

        // Create header panel to hold tab buttons.
        headerPanel = new JPanel();
        headerPanel.setBackground(headerColor);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, dividerColor));
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0 0",
                "",                  // col constraints
                "align top,fill"));  // row constraints
        
        // Create panel to hold tab content panels.
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
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
     * @param tabId identifier for initial tab, one of TabId.CONNECTIONS, 
     *  LOGGING, or CONSOLE
     */
    public void display(TabId tabId) {
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

            // Select initial tab.
            select(tabId);
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
                TabPanel tabPanel = tabPanelMap.get(tabId);
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
     * Selects the tab item with the specified identifier.
     * @param tabId the identifier of the tab
     */
    private void select(TabId tabId) {
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
        
        // Start refresh updates in tab.
        tabPanel.start();
        
        // Fire event to select tab.
        selectedItem.fireSelected(true);

        // Display selected tab panel.
        cardLayout.show(cardPanel, tabId.toString());
    }

    /**
     * Selects the next tab item.
     */
    private void selectNext() {
        // Find the selected tab, and select the next one.
        TabId[] tabIds = TabId.values();
        for (int i = 0; i < tabIds.length; i++) {
            if (tabIds[i].equals(selectedItem.getTabId())) {
                int nextTab = (i + 1) % tabIds.length;
                select(tabIds[nextTab]);
                break;
            }
        }
    }
    
    /**
     * Selects the previous tab item.
     */
    private void selectPrev() {
        // Find the selected tab, and select the previous one.
        TabId[] tabIds = TabId.values();
        for (int i = 0; i < tabIds.length; i++) {
            if (tabIds[i].equals(selectedItem.getTabId())) {
                int prevTab = ((i == 0) ?  tabIds.length : i) - 1;
                select(tabIds[prevTab]);
                break;
            }
        }
    }
    
    /**
     * A TabItem for the Advanced Tools dialog. 
     */
    private class AdvancedTabItem extends AbstractTabItem {

        private final TabId tabId;
        private final Icon icon;
        private final Provider<? extends TabPanel> provider;

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
        
        public Icon getIcon() {
            return icon;
        }
        
        public TabId getTabId() {
            return tabId;
        }

        public TabPanel getTabPanel() {
            return this.provider.get();
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
            putValue(Action.ACTION_COMMAND_KEY, tabId.toString());

            // Install listener to change state when tab item is selected. 
            tabItem.addTabItemListener(new TabItemListener() {
                @Override
                public void itemSelected(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                }
            });
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
