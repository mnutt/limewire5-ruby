package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TabbedPaneUI;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.PlayerSettings;
import org.limewire.core.settings.SWTBrowserSettings;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIMediator.Tabs;
import com.limegroup.gnutella.gui.connection.ConnectionMediator;
import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.dnd.TransferHandlerDropTargetListener;
import com.limegroup.gnutella.gui.download.DownloadMediator;
import com.limegroup.gnutella.gui.library.LibraryMediator;
import com.limegroup.gnutella.gui.logging.LoggingMediator;
import com.limegroup.gnutella.gui.menu.MenuMediator;
import com.limegroup.gnutella.gui.options.OptionsMediator;
import com.limegroup.gnutella.gui.playlist.PlaylistMediator;
import com.limegroup.gnutella.gui.search.MagnetClipboardListener;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.tabs.ConnectionsTab;
import com.limegroup.gnutella.gui.tabs.ConsoleTab;
import com.limegroup.gnutella.gui.tabs.LibraryPlayListTab;
import com.limegroup.gnutella.gui.tabs.LoggingTab;
import com.limegroup.gnutella.gui.tabs.MonitorUploadTab;
import com.limegroup.gnutella.gui.tabs.SwingBrowserSearchTab;
import com.limegroup.gnutella.gui.tabs.SearchDownloadTab;
import com.limegroup.gnutella.gui.tabs.Tab;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.gui.upload.UploadMediator;
import com.limegroup.gnutella.util.LogUtils;

/**
 * This class constructs the main <tt>JFrame</tt> for the program as well as 
 * all of the other GUI classes.  
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class MainFrame implements RefreshListener, ThemeObserver {

    /**
     * Handle to the <tt>JTabbedPane</tt> instance.
     */
    private final JTabbedPane TABBED_PANE =
        new JTabbedPane();

    /**
     * Constant handle to the <tt>SearchMediator</tt> class that is
     * responsible for displaying search results to the user.
     */
    private final SearchMediator SEARCH_MEDIATOR =
        new SearchMediator();

     /**
     * Constant handle to the <tt>DownloadMediator</tt> class that is
     * responsible for displaying active downloads to the user.
     */
    private final DownloadMediator DOWNLOAD_MEDIATOR =
        DownloadMediator.instance();

    /**
     * Constant handle to the <tt>MonitorView</tt> class that is
     * responsible for displaying incoming search queries to the user.
     */
    private final MonitorView MONITOR_VIEW =
        new MonitorView();
    
    /**
     * Constant handle to the <tt>UploadMediator</tt> class that is
     * responsible for displaying active uploads to the user.
     */
    private final UploadMediator UPLOAD_MEDIATOR =
        UploadMediator.instance();

    /**
     * Constant handle to the <tt>ConnectionView</tt> class that is
     * responsible for displaying current connections to the user.
     */
    private final ConnectionMediator CONNECTION_MEDIATOR =
        ConnectionMediator.instance();

    /**
     * Constant handle to the <tt>LibraryView</tt> class that is
     * responsible for displaying files in the user's repository.
     */
    private final LibraryMediator LIBRARY_MEDIATOR =
        LibraryMediator.instance();
    
    private final LoggingMediator LOGGING_MEDIATOR =
        LoggingMediator.instance();

    /**
     * Constant handle to the <tt>OptionsMediator</tt> class that is
     * responsible for displaying customizable options to the user.
     */
    private final OptionsMediator OPTIONS_MEDIATOR =
        OptionsMediator.instance();

    /**
     * Constant handle to the <tt>StatusLine</tt> class that is
     * responsible for displaying the status of the network and
     * connectivity to the user.
     */
    private final StatusLine STATUS_LINE = new StatusLine(GuiCoreMediator.getNetworkManager());

    /**
     * Handle the <tt>MenuMediator</tt> for use in changing the menu
     * depending on the selected tab.
     */
    private final MenuMediator MENU_MEDIATOR =
        MenuMediator.instance();

    /**
     * The main <tt>JFrame</tt> for the application.
     */
    private final JFrame FRAME;

    /**
     * Is the download view currently being shown? 
     */
    private boolean isDownloadViewVisible = false;

    /**
     * Constant for the <tt>LogoPanel</tt> used for displaying the
     * lime/spinning lime search status indicator and the logo.
     */
    private final LogoPanel LOGO_PANEL = new LogoPanel();

    /**
     * The array of tabs in the main application window.
     */
    private Map<GUIMediator.Tabs, Tab> TABS = new EnumMap<GUIMediator.Tabs, Tab>(Tabs.class);

	private int height;

	private boolean isSearching = false;
    
    /**
     * The last state of the X/Y location and the time it was set.
     * This is necessary to preserve the maximize size & prior size,
     * as on Windows a move event is occasionally triggered when
     * maximizing, prior to the state actually becoming maximized.
     */
    private WindowState lastState = null;
    
   
    /** simple state. */
    private static class WindowState {
        private final int x;
        private final int y;
        private final long time;
        WindowState() {
            x = ApplicationSettings.WINDOW_X.getValue();
            y = ApplicationSettings.WINDOW_Y.getValue();
            time = System.currentTimeMillis();
        }
    }

    @InspectionPoint("selectedMainTabs")
    private static InspectionHistogram<String> selectedTabIndices = new InspectionHistogram<String>();

    @InspectionPoint("mainTabSelectionTimes")
    private static InspectionHistogram<String> relativeSelectionTimes = new InspectionHistogram<String>();
    
    /** 
     * Initializes the primary components of the main application window,
     * including the <tt>JFrame</tt> and the <tt>JTabbedPane</tt>
     * contained in that window.
     */
    MainFrame(JFrame frame) {
        FRAME = frame;
        new DropTarget(FRAME, new TransferHandlerDropTargetListener(DNDUtils.DEFAULT_TRANSFER_HANDLER));

        // Setup the Tabs structure based on advertising mode and Windows
        buildTabs();

        TABBED_PANE.setPreferredSize(new Dimension(10000, 10000));        
        
        // Add a listener for saving the dimensions of the window &
        // position the search icon overlay correctly.
        FRAME.addComponentListener(new ComponentListener() {
            public void componentHidden(ComponentEvent e) {}
            
            public void componentShown(ComponentEvent e) {
                setSearchIconLocation();
            }
            
            public void componentMoved(ComponentEvent e) {
                lastState = new WindowState();
                saveWindowState();
            }

            public void componentResized(ComponentEvent e) {
                saveWindowState();
                setSearchIconLocation();
            }
        });

        // Listen for the size/state changing.
        FRAME.addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                saveWindowState();
            }
        });
 
        // Listen for the window closing, to save settings.
        FRAME.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeiconified(WindowEvent e) {
                // Handle reactivation on systems which do not support
                // the system tray.  Windows systems call the
                // WindowsNotifyUser.restoreApplication()
                // method to restore applications from minimize and
                // auto-shutdown modes.  Non-windows systems restore
                // the application using the following code.
                if(!OSUtils.supportsTray() || !ResourceManager.instance().isTrayIconAvailable())
                    GUIMediator.restoreView();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowState();
                SettingsGroupManager.instance().save();
                GUIMediator.close(true);
            }

        });

        FRAME.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setFrameDimensions();


        // listener for updating the tab's titles & tooltips.
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Tab tab = (Tab)evt.getSource();
                int idx = getTabIndex(tab);
                if(idx != -1) {
                    if("title".equals(evt.getPropertyName()))
                        TABBED_PANE.setTitleAt(idx, (String)evt.getNewValue());
                    else if("tooltip".equals(evt.getPropertyName()))
                        TABBED_PANE.setToolTipTextAt(idx, (String)evt.getNewValue());
                }
            }
        };
        
        // add all tabs initially....
        for(GUIMediator.Tabs tab : GUIMediator.Tabs.values()) {
            Tab t = TABS.get(tab);
            if(t != null) {
                this.addTab(t);
                t.addPropertyChangeListener(propertyChangeListener);
            }
        }

        TABBED_PANE.setRequestFocusEnabled(false);

        TABBED_PANE.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                TabbedPaneUI ui = TABBED_PANE.getUI();
                int idx = ui.tabForCoordinate(TABBED_PANE, e.getX(), e.getY());
                if(idx != -1) {
                    Tab tab = getTabForIndex(idx);
                    if(tab != null)
                        tab.mouseClicked();
                }
            }
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
        });          

        // remove tabs according to Settings Manager...
        if (!ApplicationSettings.MONITOR_VIEW_ENABLED.getValue())
            this.setTabVisible(GUIMediator.Tabs.MONITOR, false);
        if (!ApplicationSettings.CONNECTION_VIEW_ENABLED.getValue())
            this.setTabVisible(GUIMediator.Tabs.CONNECTION, false);
        if (!ApplicationSettings.LIBRARY_VIEW_ENABLED.getValue())
            this.setTabVisible(GUIMediator.Tabs.LIBRARY, false);
        if (LogUtils.isLog4JAvailable()) {
            if (!ApplicationSettings.CONSOLE_VIEW_ENABLED.getValue())
                this.setTabVisible(GUIMediator.Tabs.CONSOLE, false);
        }
        if(!ApplicationSettings.LOGGING_VIEW_ENABLED.getValue())
            this.setTabVisible(GUIMediator.Tabs.LOGGING, false);
        if( SWTBrowserSettings.USE_SWT_BROWSER.getValue()&& GUIMediator.isBrowserCapable()) {
            if(!ApplicationSettings.SWT_BROWSER_VIEW_ENABLED.getValue())
                this.setTabVisible(GUIMediator.Tabs.LWS, false);
        }

        FRAME.setJMenuBar(MENU_MEDIATOR.getMenuBar());
        JPanel contentPane = new JPanel();
        FRAME.setContentPane(contentPane);
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        contentPane.add(TABBED_PANE, gbc);
        gbc.weighty = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(STATUS_LINE.getComponent(), gbc);
      

        JLayeredPane layeredPane =
            JLayeredPane.getLayeredPaneAbove(TABBED_PANE);
        layeredPane.add(LOGO_PANEL, JLayeredPane.PALETTE_LAYER, 0);

        ThemeMediator.addThemeObserver(this);
        GUIMediator.addRefreshListener(this);

        updateLogoHeight();
        
        if (ApplicationSettings.MAGNET_CLIPBOARD_LISTENER.getValue()) {
            FRAME.addWindowListener(MagnetClipboardListener.getInstance());
        }
        
        PowerManager pm = new PowerManager();
        FRAME.addWindowListener(pm);
        GUIMediator.addRefreshListener(pm);
        
        // inspection point for selected tabs
        TABBED_PANE.getModel().addChangeListener(new InspectionChangeListener());
    }
    
    /** Saves the state of the Window to settings. */
    void saveWindowState() {
        int state = FRAME.getExtendedState();
        if(state == Frame.NORMAL) {
            // save the screen size and location 
            Dimension dim = GUIMediator.getAppSize();
            if((dim.height > 100) && (dim.width > 100)) {
                Point loc = GUIMediator.getAppLocation();
                ApplicationSettings.APP_WIDTH.setValue(dim.width);
                ApplicationSettings.APP_HEIGHT.setValue(dim.height);
                ApplicationSettings.WINDOW_X.setValue(loc.x);
                ApplicationSettings.WINDOW_Y.setValue(loc.y);
                ApplicationSettings.MAXIMIZE_WINDOW.setValue(false);
            }
        } else if( (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            ApplicationSettings.MAXIMIZE_WINDOW.setValue(true);
            if(lastState != null && lastState.time == System.currentTimeMillis()) {
                ApplicationSettings.WINDOW_X.setValue(lastState.x);
                ApplicationSettings.WINDOW_Y.setValue(lastState.y);
                lastState = null;
            }
        }
    }

    // inherit doc comment
    public void updateTheme() {
        FRAME.setJMenuBar(MENU_MEDIATOR.getMenuBar());
        LOGO_PANEL.updateTheme();
        setSearchIconLocation();
        updateLogoHeight();
        for(GUIMediator.Tabs tab : GUIMediator.Tabs.values())
            updateTabIcon(tab);
	}
    
    private void updateLogoHeight() {
        // necessary so that the logo does not intrude on the content below
        Rectangle rect = TABBED_PANE.getUI().getTabBounds(TABBED_PANE, 0);
        Dimension ld = LOGO_PANEL.getPreferredSize();
        int height = ld.height + 4;
		this.height = Math.max(rect.height, height);
        if (rect.height < height)
            TABBED_PANE.setBorder(BorderFactory.createEmptyBorder(
                height - rect.height, 0, 0, 0));
        else
            TABBED_PANE.setBorder(null);
    }

    /**
     * Build the Tab Structure based on advertising mode and Windows
     */
    private void buildTabs() {
        TABS.put(GUIMediator.Tabs.SEARCH, new SearchDownloadTab(SEARCH_MEDIATOR, DOWNLOAD_MEDIATOR));
        TABS.put(GUIMediator.Tabs.MONITOR, new MonitorUploadTab(MONITOR_VIEW, UPLOAD_MEDIATOR));
        TABS.put(GUIMediator.Tabs.CONNECTION, new ConnectionsTab(CONNECTION_MEDIATOR));
        TABS.put(GUIMediator.Tabs.LIBRARY, new LibraryPlayListTab(LIBRARY_MEDIATOR));
        TABS.put(GUIMediator.Tabs.LOGGING, new LoggingTab(LOGGING_MEDIATOR));
        if (SWTBrowserSettings.USE_SWT_BROWSER.getValue() && GUIMediator.isBrowserCapable() ) {
            TABS.put(GUIMediator.Tabs.LWS, new SwingBrowserSearchTab());
        }
        
        if (LogUtils.isLog4JAvailable()) {
            Console console = new Console();
            TABS.put(GUIMediator.Tabs.CONSOLE, new ConsoleTab(console));
        }        
    }

    
    /**
     * Adds a tab to the <tt>JTabbedPane</tt> based on the data supplied
     * in the <tt>Tab</tt> instance.
     *
     * @param tab the <tt>Tab</tt> instance containing data for the tab to
     *  add
     */
    private void addTab(Tab tab) {
        TABBED_PANE.addTab(tab.getTitle(), tab.getIcon(),
                           tab.getComponent(), tab.getToolTip());
    }

    /**
     * Inserts a tab in the <tt>JTabbedPane</tt> at the specified index, 
     * based on the data supplied in the <tt>Tab</tt> instance.
     *
     * @param tab the <tt>Tab</tt> instance containing data for the tab to
     *  add
     */
    private void insertTab(Tab tab, int index) {
        TABBED_PANE.insertTab(tab.getTitle(), tab.getIcon(),
                              tab.getComponent(), tab.getToolTip(),
                              index);
        // the component tree must be updated so that the new tab
        // fits the current theme (if the theme was changed at runtime)
        SwingUtilities.updateComponentTreeUI(TABBED_PANE);
        ThemeMediator.updateThemeObservers();
    }

    /**
     * Sets the selected index in the wrapped <tt>JTabbedPane</tt>.
     *
     * @param index the tab index to select
     */
    public final void setSelectedTab(GUIMediator.Tabs tab) {
        int i = getTabIndex(tab);
        if (i == -1)
            return;
        TABBED_PANE.setSelectedIndex(i);
    }

    /** Updates the icon in a tab. */
    void updateTabIcon(GUIMediator.Tabs tab) {
        int i = getTabIndex(tab);
        if (i != -1) {
            Tab t = TABS.get(tab);
            if(t != null)
                TABBED_PANE.setIconAt(i, t.getIcon());
        }
    }

    /**
     * Sets the x,y location as well as the height and width of the main
     * application <tt>Frame</tt>.
     */
    private final void setFrameDimensions() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        int locX = 0;
        int locY = 0;

        int appWidth  = Math.min(screenSize.width-insets.left-insets.right, ApplicationSettings.APP_WIDTH.getValue());
        int appHeight = Math.min(screenSize.height-insets.top-insets.bottom, ApplicationSettings.APP_HEIGHT.getValue());
        
        // Set the location of our window based on whether or not
        // the user has run the program before, and therefore may have 
        // modified the location of the main window.
//        if(ApplicationSettings.RUN_ONCE.getValue()) {
//            locX = Math.max(insets.left, ApplicationSettings.WINDOW_X.getValue());
//            locY = Math.max(insets.top, ApplicationSettings.WINDOW_Y.getValue());
//        } else {
//            locX = (screenSize.width - appWidth) / 2;
//            locY = (screenSize.height - appHeight) / 2;
//        }
        
        // Make sure the Window is visible and not for example 
        // somewhere in the very bottom right corner.
        if (locX+appWidth > screenSize.width) {
            locX = Math.max(insets.left, screenSize.width - insets.left - insets.right - appWidth);
        }
        
        if (locY+appHeight > screenSize.height) {
            locY = Math.max(insets.top, screenSize.height - insets.top - insets.bottom - appHeight);
        }
        
        FRAME.setLocation(locX, locY);
        FRAME.setSize(new Dimension(appWidth, appHeight));
        FRAME.getContentPane().setSize(new Dimension(appWidth, appHeight));
        ((JComponent)FRAME.getContentPane()).setPreferredSize(new Dimension(appWidth, appHeight));
        
        //re-maximize if we shutdown while maximized.
        if(ApplicationSettings.MAXIMIZE_WINDOW.getValue() 
                && Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            FRAME.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }


    /**
     * Sets the visible/invisible state of the tab associated with the
     * specified index.  The indeces correspond to the order of the
     * tabs whether or not they are visible, as specified in 
     * <tt>GUIMediator</tt>.
     *
     * @param TAB_INDEX the index of the tab to make visible or 
     *  invisible
     * @param VISIBLE the visible/invisible state to set the tab to
     */
    void setTabVisible(GUIMediator.Tabs tabItem, boolean visible) {
        Tab tab = TABS.get(tabItem);
        Component comp = tab.getComponent();
        int tabCount = TABBED_PANE.getTabCount();
        
        if (!visible) {
            // remove the tab from the tabbed pane
            for (int i = 0; i < tabCount; i++) {
                if (comp.equals(TABBED_PANE.getComponentAt(i))) {
                    TABBED_PANE.remove(i);
                    break;
                }
            }
        } else {
            // make sure the current one is invisible.
       //     JComponent selComp =
       //         (JComponent)TABBED_PANE.getSelectedComponent();
       //     selComp.setVisible(false);
            
            // We need to insert the tab in the right order,
            // according to the ordinal value of the enum.
            // To do this, we iterate through the visible tabs
            // and insert the new tab once we encounter
            // a visible tab whose 'Tab' counterpart has an
            // ordinal higher than ours.
            // (If we reached the end of the visible tabs
            //  without finding a higher ordinal, we insert
            //  at the end.)
            
            
            int ordinal = tabItem.ordinal();
            
            // add the tab to the tabbed pane
            for (int i = 0; i < tabCount; i++) {                
                Component c = TABBED_PANE.getComponentAt(i);
                int o = getOrdinalForTabComponent(c);
                if(o > ordinal) { // reached a higher tab
                    insertTab(tab, i);
                    break;
                } else if(i == tabCount - 1) { // at end of list
                    insertTab(tab, i+1);
                }
            }
            
            JComponent jcomp = (JComponent)comp;
            jcomp.invalidate();
            jcomp.revalidate();
            jcomp.repaint();
        }

        tabItem.setEnabled(visible);
        tab.storeState(visible);
    }
    
    /**
     * Returns the ordinal of the enum that points to the tab
     * holding the given component.
     */
    private int getOrdinalForTabComponent(Component c) {
        for(Map.Entry<GUIMediator.Tabs, Tab> entry : TABS.entrySet()) {
            if(entry.getValue().getComponent().equals(c))
                return entry.getKey().ordinal();
        }
        return -1;
    }

    /**
     * Returns the index in the tabbed pane of the specified "real" index
     * argument.  The values for this argument are listed in
     * <tt>GUIMediator</tt>.
     *
     * @param index the "real" index of the tab, meaning that this index
     *  is independent of what is currently visible in the tab
     * @return the index in the tabbed pane of the specified real index,
     *  or -1 if the specified index is not found
     */
    private int getTabIndex(GUIMediator.Tabs tab) {
        Tab t = TABS.get(tab);
        if(t != null) {
            return getTabIndex(t);
        } else {
            return -1;
        }
    }
    
    private int getTabIndex(Tab tab) {
        int tabCount = TABBED_PANE.getTabCount();
        Component comp = tab.getComponent();
        for (int i = 0; i < tabCount; i++) {
            Component tabComp = TABBED_PANE.getComponentAt(i);
            if (tabComp.equals(comp))
                return i;
        }
        return -1;
    }
    
    /**
     * Returns the tab associated with the visual index.
     * 
     * @param idx
     * @return
     */
    private Tab getTabForIndex(int idx) {
       Component c = TABBED_PANE.getComponentAt(idx);
       if(c == null)
           return null;
       
       for(Tab tab : TABS.values()) {
           if(tab.getComponent() != null && tab.getComponent().equals(c))
               return tab;
       }
       
       return null;
    }

    /**
     * Should be called whenever state may have changed, so MainFrame can then
     * re-layout window (if necessary).
     */
    public void refresh() {

		if (isSearching) {
			// if we're searching make sure the search result panel
			// is visible
		    SearchDownloadTab tab = (SearchDownloadTab)TABS.get(GUIMediator.Tabs.SEARCH);
			if (tab.getDividerLocation() == 0) {
				tab.setDividerLocation(0.5);
				isDownloadViewVisible = true;
			}
		}
		
        // first handle the download view
        if (DOWNLOAD_MEDIATOR.getActiveDownloads() == 0 &&
                isDownloadViewVisible) {
            ((SearchDownloadTab)TABS.get(GUIMediator.Tabs.SEARCH)).
                setDividerLocation(1000);
            isDownloadViewVisible = false;
        } else if (DOWNLOAD_MEDIATOR.getActiveDownloads() > 0 &&
                 !isDownloadViewVisible) {
            // need to turn it on....
            final int count = DOWNLOAD_MEDIATOR.getActiveDownloads();
            // make sure stuff didn't change on me....
            if (count > 0) {
                final double prop = (count > 6) ? 0.60 : 0.70;
                ((SearchDownloadTab)TABS.get(GUIMediator.Tabs.SEARCH)).
                    setDividerLocation(prop);
                ((SearchDownloadTab)TABS.get(GUIMediator.Tabs.SEARCH)).
                    getComponent().revalidate();
                TABBED_PANE.revalidate();
                isDownloadViewVisible = true;
            }
        }
    }

    /**
     * Returns a reference to the <tt>SearchMediator</tt> instance.
     *
     * @return a reference to the <tt>SearchMediator</tt> instance
     */
    final SearchMediator getSearchMediator() {
        return SEARCH_MEDIATOR;
    }

    /**
     * Returns a reference to the <tt>DownloadMediator</tt> instance.
     *
     * @return a reference to the <tt>DownloadMediator</tt> instance
     */
    final DownloadMediator getDownloadMediator() {
        return DOWNLOAD_MEDIATOR;
    }

    /**
     * Returns a reference to the <tt>MonitorView</tt> instance.
     *
     * @return a reference to the <tt>MonitorView</tt> instance
     */
    final MonitorView getMonitorView() {
        return MONITOR_VIEW;
    }

    /**
     * Returns a reference to the <tt>UploadMediator</tt> instance.
     *
     * @return a reference to the <tt>UploadMediator</tt> instance
     */
    final UploadMediator getUploadMediator() {
        return UPLOAD_MEDIATOR;
    }

    /**
     * Returns a reference to the <tt>ConnectionMediator</tt> instance.
     *
     * @return a reference to the <tt>ConnectionMediator</tt> instance
     */
    final ConnectionMediator getConnectionMediator() {
        return CONNECTION_MEDIATOR;
    }


    /**
     * Returns a reference to the <tt>LibraryMediator</tt> instance.
     *
     * @return a reference to the <tt>LibraryMediator</tt> instance
     */
    final LibraryMediator getLibraryMediator() {
        return LIBRARY_MEDIATOR;
    }
    
    /** Returns the logging mediator. */
    final LoggingMediator getLoggingMediator() {
        return LOGGING_MEDIATOR;
    }
    
    /**
     * Returns a reference to the <tt>PlaylistMediator</tt> instance.
     *
     * @return a reference to the <tt>PlaylistMediator</tt> instance or
     * <code>null</code> if the playlist is not enabled
     */
    static final PlaylistMediator getPlaylistMediator() {
        return PlayerSettings.PLAYER_ENABLED.getValue() ?
                PlaylistMediator.getInstance() : null;
    }    

    /**
     * Returns a reference to the <tt>StatusLine</tt> instance.
     *
     * @return a reference to the <tt>StatusLine</tt> instance
     */
    final StatusLine getStatusLine() {
        return STATUS_LINE;
    }

    /**
     * Returns a reference to the <tt>MenuMediator</tt> instance.
     *
     * @return a reference to the <tt>MenuMediator</tt> instance
     */
    final MenuMediator getMenuMediator() {
        return MENU_MEDIATOR;
    }

    /**
     * Returns a reference to the <tt>OptionsMediator</tt> instance.
     *
     * @return a reference to the <tt>OptionsMediator</tt> instance
     */
    final OptionsMediator getOptionsMediator() {
        return OPTIONS_MEDIATOR;
    }

    /**
     * Returns a reference to the <tt>StatisticsView</tt> instance.
     *
     * @return a reference to the <tt>StatisticsView</tt> instance
     */
    //final StatisticsView getStatisticsView() {
    //return STATISTICS_VIEW;
    //}

    /**
     * Sets the searching or not searching status of the application.
     *
     * @param searching the searching status of the application
     */
    final void setSearching(boolean searching) {    
        LOGO_PANEL.setSearching(searching);
		isSearching = searching;
		refresh();
    }

    /**
     * Sets the location of the search status icon.
     */
    private void setSearchIconLocation() {
		int y = MENU_MEDIATOR.getMenuBarHeight() 
			+ (height - LOGO_PANEL.getPreferredSize().height) / 2;
        LOGO_PANEL.setLocation(
            FRAME.getSize().width - LOGO_PANEL.getSize().width - 12,
            y);
    }
    
    class InspectionChangeListener implements ChangeListener {
        
        private Tab lastTab = getSelectedTab();

        private long lastChangeTime = System.currentTimeMillis();

        /**
         * @return null if there is no selected tab
         */
        private Tab getSelectedTab() {
            int index = TABBED_PANE.getSelectedIndex();
            return index != -1 ? getTabForIndex(index) : null;
        }
        
        public void stateChanged(ChangeEvent e) {
            long currentTime = System.currentTimeMillis();
            if (lastTab != null) {
                String tabName = lastTab.getIconName();
                tabName = tabName != null ? tabName : "no name";
                relativeSelectionTimes.count(tabName, currentTime - lastChangeTime);
                selectedTabIndices.count(tabName);
            }
            lastChangeTime = currentTime;
            lastTab = getSelectedTab();
        }
    }
    
}
