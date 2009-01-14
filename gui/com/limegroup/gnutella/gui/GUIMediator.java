package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.PlayerSettings;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.core.settings.SWTBrowserSettings;
import org.limewire.core.settings.StartupSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.Connectable;
import org.limewire.lws.server.LWSConnectionListener;
import org.limewire.service.ErrorService;
import org.limewire.service.Switch;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.VersionUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.bugs.FatalBugManager;
import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.connection.ConnectionMediator;
import com.limegroup.gnutella.gui.download.DownloadMediator;
import com.limegroup.gnutella.gui.library.LibraryMediator;
import com.limegroup.gnutella.gui.mp3.MediaPlayerComponent;
import com.limegroup.gnutella.gui.mp3.PlayListItem;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;
import com.limegroup.gnutella.gui.options.OptionsMediator;
import com.limegroup.gnutella.gui.playlist.PlaylistMediator;
import com.limegroup.gnutella.gui.properties.ResultProperties;
import com.limegroup.gnutella.gui.properties.ResultPropertiesDialog;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.shell.LimeAssociations;
import com.limegroup.gnutella.gui.shell.ShellAssociationManager;
import com.limegroup.gnutella.gui.tabs.LibraryPlayListTab;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.gui.upload.UploadMediator;
import com.limegroup.gnutella.util.LaunchException;
import com.limegroup.gnutella.util.Launcher;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.LogUtils;
import com.limegroup.gnutella.version.UpdateInformation;


/**
 * This class acts as a central point of access for all gui components, a sort
 * of "hub" for the frontend.  This should be the only common class that all
 * frontend components have access to, reducing the overall dependencies and
 * therefore increasing the modularity of the code.
 *
 * <p>Any functions or services that should be accessible to multiple classes
 * should be added to this class.  These currently include such functions as
 * easily displaying standardly-formatted messages to the user, obtaining
 * locale-specific strings, and obtaining image resources, among others.
 *
 * <p>All of the methods in this class should be called from the event-
 * dispatch (Swing) thread.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class GUIMediator {

    /**
     * The number of messages a connection must have sent before we consider
     * it stable for the UI.
     */
    private static final int STABLE_THRESHOLD = 5;

    /**
     * Flag for whether or not a message has been displayed to the user --
     * useful in deciding whether or not to display other dialogues.
     */
	private static boolean _displayedMessage;
    
    /**
     * Message key for the disconnected message
     */
    private static final String DISCONNECTED_MESSAGE = I18nMarker
            .marktr("Your machine does not appear to have an active Internet connection or a firewall is blocking LimeWire from accessing the internet. LimeWire will automatically keep trying to connect you to the network unless you select \"Disconnect\" from the File menu.");
    

    /**
	 * Singleton for easy access to the mediator.
	 */
	private static GUIMediator _instance = null;
    
    public static enum Tabs {
        SEARCH(I18n.tr("&Search")),
        MONITOR(I18n.tr("&Monitor"), ApplicationSettings.MONITOR_VIEW_ENABLED),
        CONNECTION(I18n.tr("&Connections"), ApplicationSettings.CONNECTION_VIEW_ENABLED),
        LIBRARY(I18n.tr("&Library"), ApplicationSettings.LIBRARY_VIEW_ENABLED),
        CONSOLE(I18n.tr("C&onsole"), ApplicationSettings.CONSOLE_VIEW_ENABLED),
        LOGGING(I18n.tr("Lo&gging"), ApplicationSettings.LOGGING_VIEW_ENABLED),
        LWS(SWTBrowserSettings.getTitleSetting(), ApplicationSettings.SWT_BROWSER_VIEW_ENABLED);
        
        private final Action navAction;
        
        private String name;
        
        private final BooleanSetting visibleSetting;
        
        private final PropertyChangeSupport propertyChangeSupport;
        
        private Tabs(String name) {
         this(name, null);   
        }
        
        private Tabs(String nameWithAmpers, BooleanSetting visibleSetting) {
            this.name = GUIUtils.stripAmpersand(nameWithAmpers);
            navAction = new NavigationAction(nameWithAmpers, I18n.tr("Display the {0} Screen", name));
            this.visibleSetting = visibleSetting;
            this.propertyChangeSupport = new PropertyChangeSupport(this);
        }
        
        private Tabs(StringSetting nameSetting, BooleanSetting visibleSetting) {
            this(nameSetting.getValue(), visibleSetting);
            nameSetting.addSettingListener(new SettingListener() {
                public void settingChanged(final SettingEvent evt) {
                    if(evt.getEventType() == SettingEvent.EventType.VALUE_CHANGED) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                setName(evt.getSetting().getValueAsString());
                            }
                        });
                    }
                }
            });
        }
        
        void setName(String newName) {
            String oldName = name;
            this.name = GUIUtils.stripAmpersand(newName);
            navAction.putValue(Action.NAME, newName);
            navAction.putValue(Action.LONG_DESCRIPTION, I18n.tr("Display the {0} Screen", name));
            propertyChangeSupport.firePropertyChange("name", oldName, name);
        }
        
        void setEnabled(boolean enabled) {
            navAction.setEnabled(enabled);
        }
        
        public Action getNavigationAction() {
            return navAction;
        }
        
        public boolean isViewEnabled() {
            if (visibleSetting == null) {
                throw new IllegalStateException("Should not be called on " + getName() + " which is a non-optional tab");
            }
            return visibleSetting.getValue();
        }
        
        public String getName() {
            return name;
        }

        private class NavigationAction extends AbstractAction {
            public NavigationAction(String name, String description) {
                super(name);
                putValue(Action.LONG_DESCRIPTION, description);
            }
            
            public void actionPerformed(ActionEvent e) {
                instance().setWindow(Tabs.this);
            }
        }
        
        /**
         * Returns the  
         */
        public static Tabs[] getOptionalTabs() {
            if (LogUtils.isLog4JAvailable()) {
                if( isBrowserCapable())
                    return new Tabs[] { MONITOR, CONNECTION, LIBRARY, CONSOLE, LOGGING, LWS };
                else
                return new Tabs[] { MONITOR, CONNECTION, LIBRARY, CONSOLE, LOGGING };
            }
            else {
                if( isBrowserCapable())
                    return new Tabs[] { MONITOR, CONNECTION, LIBRARY, LOGGING, LWS };
                else
                return new Tabs[] { MONITOR, CONNECTION, LIBRARY, LOGGING };
            }
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }
        
    }
    
	/**
     * Return true if the web browser can safely launch on the OS
     */
    public static boolean isBrowserCapable() {
        return OSUtils.isWindows();
    }
    
	/**
	 * Constant specifying whether or not the user has donated to the LimeWire
	 * project.
	 */
	private static boolean HAS_DONATED = true;

	/**
	 * The main <tt>JFrame</tt> for the application.
	 */
	private static final JFrame FRAME = new LimeJFrame();

	/**
	 * The popup menu on the icon in the sytem tray.
	 */
	private static final JPopupMenu TRAY_MENU = new TrayPopupMenu();

	/**
	 * <tt>List</tt> of <tt>RefreshListener</tt> classes to notify of UI
	 * refresh events.
	 */
	private static final List<RefreshListener> REFRESH_LIST = new ArrayList<RefreshListener>();
    
	/**
	 * String to be displayed in title bar of LW client.
	 */
	private final String APP_TITLE = I18n.tr("LimeWire");

	/**
	 * Handle to the <tt>OptionsMediator</tt> class that is responsible for
	 * displaying customizable options to the user.
	 */
	private static OptionsMediator _optionsMediator;
	
	/**
	 * The shell association manager.
	 */
	private static final Provider<ShellAssociationManager> ASSOCIATION_MANAGER =
        new AbstractLazySingletonProvider<ShellAssociationManager>() {
            @Override
            protected ShellAssociationManager createObject() {
                return new ShellAssociationManager(LimeAssociations.getSupportedAssociations());
            }
    };

	/**
	 * Constant handle to the <tt>MainFrame</tt> instance that handles
	 * constructing all of the primary gui components.
	 */
	private final MainFrame MAIN_FRAME = new MainFrame(FRAME);

	/**
	 * Constant handle to the <tt>DownloadMediator</tt> class that is
	 * responsible for displaying active downloads to the user.
	 */
	private final DownloadMediator DOWNLOAD_MEDIATOR =
		MAIN_FRAME.getDownloadMediator();

	/**
	 * Constant handle to the <tt>UploadMediator</tt> class that is
	 * responsible for displaying active uploads to the user.
	 */
	private final UploadMediator UPLOAD_MEDIATOR =
		MAIN_FRAME.getUploadMediator();

	/**
	 * Constant handle to the <tt>ConnectionMediator</tt> class that is
	 * responsible for displaying current connections to the user.
	 */
	private final ConnectionMediator CONNECTION_MEDIATOR =
		MAIN_FRAME.getConnectionMediator();

	/**
	 * Constant handle to the <tt>LibraryMediator</tt> class that is
	 * responsible for displaying files in the user's repository.
	 */
	private final LibraryMediator LIBRARY_MEDIATOR =
		MAIN_FRAME.getLibraryMediator();

	/**
	 * Constant handle to the <tt>DownloadView</tt> class that is responsible
	 * for displaying the status of the network and connectivity to the user.
	 */
	private final StatusLine STATUS_LINE =
		MAIN_FRAME.getStatusLine();

    /**
     * Flag for whether or not the app has ever been made visible during this
     * session.
     */
    private static boolean _visibleOnce = false;

    /**
     * Flag for whether or not the app is allowed to become visible.
     */
    private static boolean _allowVisible = false;

    /**
     * The last recorded idle time.
     */
    private long lastIdleTime = 0;
    
	/**
	 * Private constructor to ensure that this class cannot be constructed
	 * from another class.
	 */
	private GUIMediator() {
		FRAME.setTitle(APP_TITLE);
		_optionsMediator = MAIN_FRAME.getOptionsMediator();
	}

	/**
	 * Singleton accessor for this class.
	 *
	 * @return the <tt>GUIMediator</tt> instance
	 */
	public static synchronized GUIMediator instance() {
		if (_instance == null)
			_instance = new GUIMediator();
		return _instance;
	}

	/**
	 * Accessor for whether or not the GUIMediator has been constructed yet.
	 */
	public static boolean isConstructed() {
	    return _instance != null;
	}

	/**
	 * Notification that the the core has been initialized.
	 */
	public void coreInitialized() {
//		startTimer();
		createEventListeners();
	}
	
//	private final void startTimer() {
//		UpTimeTimer timer = new UpTimeTimer();
//		timer.startTimer();
//	}
	
	private void createEventListeners() {
        GuiCoreMediator.getLWSManager().addConnectionListener(new LWSConnectionListener() {
            public void connectionChanged(boolean isConnected) {
                GUIMediator.instance().getStatusLine().updateLWSLabel(!isConnected);
            }
        });
            
		//TorrentUploadCanceller.createAndRegister(GuiCoreMediator.getTorrentManager());
	}

	/**
	 * Returns a boolean specifying whether or not the wrapped
	 * <tt>JFrame</tt> is visible or not.
	 *
	 * @return <tt>true</tt> if the <tt>JFrame</tt> is visible,
	 *  <tt>false</tt> otherwise
	 */
	public static final boolean isAppVisible() {
		return FRAME.isShowing();
	}

	/**
	 * Specifies whether or not the main application window should be visible
	 * or not.
	 *
	 * @param visible specifies whether or not the application should be
	 *                made visible or not
	 */
	public static final void setAppVisible(final boolean visible) {
        safeInvokeLater(new Runnable() {
            public void run() {
                try {
                    if (visible)
                        FRAME.toFront();
                    FRAME.setVisible(visible);
                } catch (NullPointerException npe) {
                    //  NPE being thrown on WinXP sometimes.  First try
                    //  reverting to the limewire theme.  If NPE still
                    //  thrown, tell user to change LimeWire's Windows
                    //  compatibility mode to Win2k.
                    //  null pointer found
                	// Update: no idea if the NPE also happens on vista, use the workaround
                	// just in case.
                    if (OSUtils.isNativeThemeWindows()) {
                        try {
                            if (ThemeSettings.isWindowsTheme()) {
                                ThemeMediator.changeTheme(ThemeSettings.LIMEWIRE_THEME_FILE);
                                try {
                                    if (visible)
                                        FRAME.toFront();
                                    FRAME.setVisible(visible);
                                } catch (NullPointerException npe2) {
                                    GUIMediator.showError(I18n.tr("LimeWire has encountered a problem during startup and cannot proceed. You may be able to fix this problem by changing LimeWire\'s Windows Compatibility. Right-click on the LimeWire icon on your Desktop and select \'Properties\' from the popup menu. Click the \'Compatibility\' tab at the top, then click the \'Run this program in compatibility mode for\' check box, and then select \'Windows 2000\' in the box below the check box. Then click the \'OK\' button at the bottom and restart LimeWire."));
                                    System.exit(0);
                                }                                
                            } else {
                                GUIMediator.showError(I18n.tr("LimeWire has encountered a problem during startup and cannot proceed. You may be able to fix this problem by changing LimeWire\'s Windows Compatibility. Right-click on the LimeWire icon on your Desktop and select \'Properties\' from the popup menu. Click the \'Compatibility\' tab at the top, then click the \'Run this program in compatibility mode for\' check box, and then select \'Windows 2000\' in the box below the check box. Then click the \'OK\' button at the bottom and restart LimeWire."));
                                System.exit(0);
                            }
                        } catch (Throwable t) {
                            if (visible)
                                FatalBugManager.handleFatalBug(npe);
                            else
                                ErrorService.error(npe);
                        }
                    } else {
                        if (visible)
                            FatalBugManager.handleFatalBug(npe);
                        else
                            ErrorService.error(npe);
                    }
                } catch(Throwable t) {
                    if (visible)
                        FatalBugManager.handleFatalBug(t);
                    else
                        ErrorService.error(t);
                }
                if (visible) {
                    SearchMediator.requestSearchFocus();
                    // forcibily revalidate the FRAME
                    // after making it visible.
                    // on Java 1.5, it does not validate correctly.
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            FRAME.getContentPane().invalidate();
                            FRAME.getContentPane().validate();
                        }
                    });
                }
                // If the app has already been made visible, don't display extra
                // dialogs.  We could display the pro dialog here, but it causes
                // some odd issues when LimeWire is brought back up from the tray
                if (visible && !_visibleOnce) {
                    // Show the startup dialogs in the swing thread.
                    showDialogsForFirstVisibility();
                    _visibleOnce = true;
                }
            }
        });
	}

	/** Displays the search result properties dialog box. */
    public static void showProperties(ResultProperties resultProperties) {
        ResultPropertiesDialog dialog = new ResultPropertiesDialog(resultProperties);
        GUIUtils.centerOnScreen(dialog.getDialog());
        GUIUtils.addHideAction(dialog.getDialog());
        dialog.getDialog().setVisible(true);
    }

	/**
	 * Displays various dialog boxes that should only be shown the first
	 * time the application is made visible.
	 */
	private static final void showDialogsForFirstVisibility() {
		if (_displayedMessage)
			return;
		_displayedMessage = true;

		getAssociationManager().checkAndGrab(true);
		
		if (!hasDonated())
			UpgradeWindow.showProDialog();

		if (TipOfTheDayMessages.hasLocalizedMessages() && StartupSettings.SHOW_TOTD.getValue()) {
            // Construct it first...
            TipOfTheDayMediator.instance();
            
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) { }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            TipOfTheDayMediator.instance().displayTipWindow();
                        }
                    });
                }
            }, "TOTD");
        }
		
		JDialog dialog = JavaVersionNotice.getUpgradeRecommendedDialog(VersionUtils.getJavaVersion()); 
		if (dialog != null) {
		    dialog.setVisible(true);
		}
	}

	/**
	 * Displays a dialog the first time a user performs a download. 
	 * Returns true iff the user selects 'Yes'; returns false otherwise.
	 */
	public static boolean showFirstDownloadDialog() {
		if (DialogOption.YES ==
			showYesNoCancelMessage(I18n.tr("LimeWire is unable to find a license for this file. Download the file anyway?\n\nPlease note: LimeWire cannot monitor or control the content of the Gnutella network. Please respect your local copyright laws."),
                    QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING))
			return true;
		return false;
	}
	
    /**
     * Closes any dialogues that are displayed at startup and sets the flag to
     * indicate that we've displayed a message.
     */
    public static void closeStartupDialogs() {
        if(SplashWindow.instance().isShowing())
            SplashWindow.instance().toBack();
        if(TipOfTheDayMediator.isConstructed())
            TipOfTheDayMediator.instance().hide();
    }

	/**
	 * Returns a <tt>Dimension</tt> instance containing the dimensions of the
	 * wrapped JFrame.
	 *
	 * @return a <tt>Dimension</tt> instance containing the width and height
	 *         of the wrapped JFrame
	 */
	public static final Dimension getAppSize() {
		return FRAME.getSize();
	}

	/**
	 * Returns a <tt>Point</tt> instance containing the x, y position of the
	 * wrapped <ttJFrame</tt> on the screen.
	 *
	 * @return a <tt>Point</tt> instance containting the x, y position of the
	 *         wrapped JFrame
	 */
	public static final Point getAppLocation() {
		return FRAME.getLocation();
	}

	/**
	 * Returns the <tt>MainFrame</tt> instance.  <tt>MainFrame</tt> maintains
	 * handles to all of the major gui classes.
	 *
	 * @return the <tt>MainFrame</tt> instance
	 */
	public final MainFrame getMainFrame() {
		return MAIN_FRAME;
	}

	/**
	 * Returns the main application <tt>JFrame</tt> instance.
	 *
	 * @return the main application <tt>JFrame</tt> instance
	 */
	public static final JFrame getAppFrame() {
		return FRAME;
	}
	
	/**
	 * Returns the popup menu on the icon in the system tray.
	 * 
	 * @return The tray popup menu
	 */
	public static final JPopupMenu getTrayMenu() {
		return TRAY_MENU;
	}

	/**
	 * Returns the status line instance for other classes to access 
	 */
	public StatusLine getStatusLine() {
		return STATUS_LINE;
	}
	
	/**
	 * Refreshes the various gui components that require refreshing.
	 */
	public final void refreshGUI() {
		for (int i = 0; i < REFRESH_LIST.size(); i++) {
            try {
                REFRESH_LIST.get(i).refresh();
            } catch(Throwable t) {
                // Show the error for each RefreshListener individually
                // so that we continue refreshing the other items.
                ErrorService.error(t);
            }
		}

        // update the status panel
        int sharedFiles  = GuiCoreMediator.getFileManager().getGnutellaFileList().size();
        int quality      = getConnectionQuality();
        STATUS_LINE.setStatistics(sharedFiles);
        if(quality != StatusLine.STATUS_DISCONNECTED 
                && quality != StatusLine.STATUS_CONNECTING) {
            hideDisposableMessage(DISCONNECTED_MESSAGE);
        }
        
        updateConnectionUI(quality);
	}

    /**
     * Returns the connectiong quality.
     */
    public int getConnectionQuality() {
        int stable =
            GuiCoreMediator.getConnectionServices().countConnectionsWithNMessages(STABLE_THRESHOLD);
            
        int status;

        if(stable == 0) {
            int initializing = CONNECTION_MEDIATOR.getConnectingCount();
            int connections = GuiCoreMediator.getConnectionServices().getNumInitializedConnections();
            // No initializing or stable connections
            if(initializing == 0 && connections == 0) {
                //Not attempting to connect at all...
                if(!GuiCoreMediator.getConnectionServices().isConnecting())
                    status = StatusLine.STATUS_DISCONNECTED;
                //Attempting to connect...
                else
                    status = StatusLine.STATUS_CONNECTING;
            }
            // No initialized, all initializing - connecting
            else if(connections == 0)
                status = StatusLine.STATUS_CONNECTING;
            // Some initialized - poor connection.
            else
                status = StatusLine.STATUS_POOR;
        } else if(GuiCoreMediator.getConnectionManager().isConnectionIdle()) {
            lastIdleTime = System.currentTimeMillis();
            status = StatusLine.STATUS_IDLE;
        } else {
            int preferred = GuiCoreMediator.getConnectionManager().
                            getPreferredConnectionCount();
            // pro will have more.
            if(LimeWireUtils.isPro())
                preferred -= 2;
            // ultrapeers don't need as many...
            if(GuiCoreMediator.getConnectionServices().isSupernode())
                preferred -= 5;
            preferred = Math.max(1, preferred); // prevent div by 0

            double percent = (double)stable / (double)preferred;
            if(percent <= 0.25)
                status = StatusLine.STATUS_POOR;
            else if(percent <= 0.5)
                status = StatusLine.STATUS_FAIR;
            else if(percent <= 0.75)
                status = StatusLine.STATUS_GOOD;
            else if(percent <= 1)
                status = StatusLine.STATUS_EXCELLENT;
            else /* if(percent > 1) */
                status = StatusLine.STATUS_TURBOCHARGED;
        }
        
        switch(status) {
        case StatusLine.STATUS_CONNECTING:            
        case StatusLine.STATUS_POOR:
        case StatusLine.STATUS_FAIR:
        case StatusLine.STATUS_GOOD:
            // if one of these four, see if we recently woke up from
            // idle, and if so, report as 'waking up' instead.
            long now = System.currentTimeMillis();
            if(now < lastIdleTime + 15 * 1000)
                status = StatusLine.STATUS_WAKING_UP;
        }
        
        return status;
    }


	/**
	 * Sets the visibility state of the options window.
	 *
	 * @param visible the visibility state to set the window to
	 */
	public void setOptionsVisible(boolean visible) {
		if (_optionsMediator == null) return;
		_optionsMediator.setOptionsVisible(visible);
	}

	/**
	 * Sets the visibility state of the options window, and sets
	 * the selection to a option pane associated with a given key.
	 *
	 * @param visible the visibility state to set the window to
	 * @param key the unique identifying key of the panel to show
	 */
	public void setOptionsVisible(boolean visible, final String key) {
		if (_optionsMediator == null) return;
		_optionsMediator.setOptionsVisible(visible, key);
	}

	/**
	 * Returns whether or not the options window is visible
	 *
	 * @return <tt>true</tt> if the options window is visible,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isOptionsVisible() {
		if (_optionsMediator == null) return false;
		return _optionsMediator.isOptionsVisible();
	}

	/**
	 * Gets a handle to the options window main <tt>JComponent</tt> instance.
	 *
	 * @return the options window main <tt>JComponent</tt>, or <tt>null</tt>
	 *  if the options window has not yet been constructed (the window is
	 *  guaranteed to be constructed if it is visible)
	 */
	public static Component getMainOptionsComponent() {
		if (_optionsMediator == null) return null;
		return _optionsMediator.getMainOptionsComponent();
	}

	/**
	 * @return the <tt>ShellAssociationManager</tt> instance.
	 */
	public static ShellAssociationManager getAssociationManager() {
		return ASSOCIATION_MANAGER.get();
	}
	
	/**
	 * Sets the tab pane to display the given tab.
	 *
	 * @param index the index of the tab to display
	 */
	public void setWindow(GUIMediator.Tabs tab) {
		MAIN_FRAME.setSelectedTab(tab);
	}

	/**
	 * Updates the icon at the specified tab index.
	 *
	 * @param index the fixed index of the tab to update
	 */
	public void updateTabIcon(GUIMediator.Tabs tab) {
		MAIN_FRAME.updateTabIcon(tab);
	}

	/**
	 * Clear the connections in the connection view.
	 */
	public void clearConnections() {
		CONNECTION_MEDIATOR.clearConnections();
	}

	/**
	 * Sets the connected/disconnected visual status of the client.
	 *
	 * @param connected the connected/disconnected status of the client
	 */
	private void updateConnectionUI(int quality) {
        STATUS_LINE.setConnectionQuality(quality);

        boolean connected =
            quality != StatusLine.STATUS_DISCONNECTED;
		if (!connected)
			this.setSearching(false);
	}

  	/**
  	 * Returns the total number of uploads for this session.
	 *
	 * @return the total number of uploads for this session
  	 */
  	public int getTotalUploads() {
  		return UPLOAD_MEDIATOR.getTotalUploads();
  	}

  	/**
  	 * Returns the total number of currently active uploads.
	 *
	 * @return the total number of currently active uploads
  	 */
  	public int getCurrentUploads() {
  		return UPLOAD_MEDIATOR.getCurrentUploads();
  	}

  	/**
  	 * Returns the total number of downloads for this session.
	 *
	 * @return the total number of downloads for this session
  	 */
  	public final int getTotalDownloads() {
  		return DOWNLOAD_MEDIATOR.getTotalDownloads();
  	}

  	/**
  	 * Returns the total number of currently active downloads.
	 *
	 * @return the total number of currently active downloads
  	 */
  	public final int getCurrentDownloads() {
  		return DOWNLOAD_MEDIATOR.getCurrentDownloads();
  	}
  	
  	public final void openTorrent(File torrentFile) {
  		DOWNLOAD_MEDIATOR.openTorrent(torrentFile);
		setWindow(GUIMediator.Tabs.SEARCH);
  	}
  	
  	public final void openTorrentURI(URI torrentURI) {
  		DOWNLOAD_MEDIATOR.openTorrentURI(torrentURI);
  		setWindow(GUIMediator.Tabs.SEARCH);
  	}
  	
	/**
	 * Tells the library to add a new top-level (shared) folder.
	 */
	public final void addSharedLibraryFolder() {
		LIBRARY_MEDIATOR.addSharedLibraryFolder();
	}

	/**
	 * Returns the active playlist or <code>null</code> if the playlist
	 * is not enabled.
	 */
	public static PlaylistMediator getPlayList() {
	    return MainFrame.getPlaylistMediator();
    }

    /**
     * Determines whether or not the PlaylistMediator is being used this session.
     */
    public static boolean isPlaylistVisible() {
        // If we are not constructed yet, then make our best guess as
        // to visibility.  It is actually VERY VERY important that this
        // returns the same thing throughout the entire course of the program,
        // otherwise exceptions can pop up.
        if(!isConstructed())
            return PlayerSettings.PLAYER_ENABLED.getValue();
        else
            return getPlayList() != null && PlayerSettings.PLAYER_ENABLED.getValue();
    }
    
    /**
     * Runs the appropriate methods to start LimeWire up
     * hidden.
     */
    public static void startupHidden() {
        // sends us to the system tray on windows, ignored otherwise.
        GUIMediator.showTrayIcon();
        // If on OSX, we must set the framestate appropriately.
        if(OSUtils.isMacOSX())
            GUIMediator.hideView();
    }

    /**
     * Notification that visibility is now allowed.
     */
    public static void allowVisibility() {
		if(!_allowVisible && OSUtils.isAnyMac())
		    MacEventHandler.instance().enablePreferences();
        _allowVisible = true;
    }

    /**
     * Notification that loading is finished.  Updates the status line and 
     * bumps the AWT thread priority.
     */
    public void loadFinished() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Thread awt = Thread.currentThread();
                awt.setPriority(awt.getPriority() + 1);
                STATUS_LINE.loadFinished();
            }
       });
    }

    /**
     * Handles a 'reopen' event appropriately.
     * Used primarily for allowing LimeWire to be made
     * visible after it was started from system startup
     * on OSX.
     */
    public static void handleReopen() {
        // Do not do anything
        // if visibility is not allowed yet, as initialization
        // is not yet finished.
        if(_allowVisible) {
            if(!_visibleOnce)
                restoreView(); // First make sure it's not minimized
            setAppVisible(true); // Then make it visible
            // Otherwise (if the above operations were reversed), a tiny
            // LimeWire icon would appear in the 'minimized' area of the dock
            // for a split second, and the Console would report strange errors
        }
    }

	/**
	 * Hides the GUI by either sending it to the System Tray or
	 * minimizing the window.  Mimimize behavior occurs on platforms
	 * which do not support the System Tray.
	 * @see restoreView
	 */
	public static void hideView() {
        FRAME.setState(Frame.ICONIFIED);

		if (OSUtils.supportsTray() && ResourceManager.instance().isTrayIconAvailable())
			GUIMediator.setAppVisible(false);
	}


	/**
	 * Makes the GUI visible by either restoring it from the System Tray or
	 * the task bar.
	 * @see hideView
	 */
	public static void restoreView() {
		// Frame must be visible for setState to work.  Make visible
		// before restoring.

		if (OSUtils.supportsTray() && ResourceManager.instance().isTrayIconAvailable()) {
            // below is a little hack to get around odd windowing
            // behavior with the system tray on windows.  This enables
            // us to get LimeWire to the foreground after it's run from
            // the startup folder with all the nice little animations
            // that we want

            // cache whether or not to use our little hack, since setAppVisible
//            // changes the value of _visibleOnce
//            boolean doHack = false;
//            if (!_visibleOnce)
//                doHack = true;
//			GUIMediator.setAppVisible(true);
//            if (ApplicationSettings.DISPLAY_TRAY_ICON.getValue())
//                GUIMediator.showTrayIcon();
//            else
//                GUIMediator.hideTrayIcon();
//            if (doHack)
//                restoreView();
		}

        // If shutdown sequence was initiated, cancel it.  Auto shutdown is
		// disabled when the GUI is visible.
		Finalizer.cancelShutdown();
		
		FRAME.setState(Frame.NORMAL);
	}

	/**
	 * Determines the appropriate shutdown behavior based on user settings.
	 * This implementation decides between exiting the application immediately,
	 * or exiting after all file transfers in progress are complete.
	 */
	public static void close(boolean fromFrame) {
		if (ApplicationSettings.MINIMIZE_TO_TRAY.getValue()) {
		    // if we want to minimize to the tray, but LimeWire wasn't
		    // able to load the tray library, then shutdown after transfers.
		    if(OSUtils.supportsTray() && !ResourceManager.instance().isTrayIconAvailable())
		        shutdownAfterTransfers();
		    else {
                applyWindowSettings();
                GUIMediator.showTrayIcon();
                hideView();
            }
        } else if (OSUtils.isMacOSX() && fromFrame) {
            //If on OSX, don't close in response to clicking on the 'X'
            //as that's not normal behaviour.  This can only be done on Java14
            //though, because we need access to the
            //com.apple.eawt.ApplicationListener.handleReOpenApplication event
            //in order to restore the GUI.
            GUIMediator.setAppVisible(false);
//        } else if (ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.getValue()) {
//			GUIMediator.shutdownAfterTransfers();
		} else {
		    shutdown();
        }
	}

	/**
	 * Shutdown the program cleanly.
	 */
	public static void shutdown() {
		Finalizer.shutdown();
	}
    
	/**
	 * Shutdown the program cleanly after all transfers in progress are
	 * complete.  Calling this method causes the GUI to be hidden while the
	 * application waits to shutdown.
	 * @see hideView
	 */
	public static void shutdownAfterTransfers() {
		Finalizer.shutdownAfterTransfers();
		GUIMediator.hideView();
	}
    
    public static void flagUpdate(String toExecute) {
        Finalizer.flagUpdate(toExecute);
    }

	/**
	 * Shows the "About" menu with more information about the program.
	 */
	public static final void showAboutWindow() {
		new AboutWindow().showDialog();
	}

	/**
	 * Shows the user notification area.  The user notification icon and
	 * tooltip created by the NotifyUser object are not modified.
	 */
	public static void showTrayIcon() {
        NotifyUserProxy.instance().showTrayIcon();
	}

    /**
     * Hides the user notification area.
     */
    public static void hideTrayIcon() {
        //  Do not use hideNotify() here, since that will
        //  create multiple tray icons.
        NotifyUserProxy.instance().hideTrayIcon();
    }

    /**
     * Sets the window height, width and location properties to remember the
     * next time the program is started.
     */
    public static void applyWindowSettings()  {
//        ApplicationSettings.RUN_ONCE.setValue(true);
        if (GUIMediator.isAppVisible()) {            
            if((GUIMediator.getAppFrame().getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                ApplicationSettings.MAXIMIZE_WINDOW.setValue(true);
            } else {
                // set the screen size and location for the
                // next time the application is run.
                Dimension dim = GUIMediator.getAppSize();
                // only save reasonable sizes to get around a bug on
                // OS X that could make the window permanently
                // invisible
                if((dim.height > 100) && (dim.width > 100)) {
                    Point loc = GUIMediator.getAppLocation();
                    ApplicationSettings.APP_WIDTH.setValue(dim.width);
                    ApplicationSettings.APP_HEIGHT.setValue(dim.height);
                    ApplicationSettings.WINDOW_X.setValue(loc.x);
                    ApplicationSettings.WINDOW_Y.setValue(loc.y);
                }
            }
        }
    }

	/**
	 * Serves as a single point of access for any icons used in the program.
	 *
	 * @param imageName the name of the icon to return without path
	 *                  information, as in "plug"
	 * @return the <tt>ImageIcon</tt> object specified in the param string
	 */
	public static final ImageIcon getThemeImage(final String name) {
		return ResourceManager.getThemeImage(name);
	}

	/**
	 * Returns an ImageIcon for the specified resource.
	 */
	public static final ImageIcon getImageFromResourcePath(final String loc) {
	    return ResourceManager.getImageFromResourcePath(loc);
    }

	/**
	 * Returns a new <tt>URL</tt> instance for the specified file name.
	 * The file must be located in the org/limewire/gui/resources
	 * directory, or this will return <tt>null</tt>.
	 *
	 * @param FILE_NAME the name of the file to return a url for without path
	 *  information, as in "about.html"
	 * @return the <tt>URL</tt> instance for the specified file, or
	 * <tt>null</tt> if the <tt>URL</tt> could not be loaded
	 */
	public static URL getURLResource(final String FILE_NAME) {
		return ResourceManager.getURLResource(FILE_NAME);
	}

	/**
	 * Resets locale options.
	 */
	public static void resetLocale() {
	    ResourceManager.resetLocaleOptions();
        GUIUtils.resetLocale();
	}

	/**
     * Return ResourceBundle for use with specific xml schema
     *
     * @param schemaname the name of schema
     *        (not the URI but name returned by LimeXMLSchema.getDisplayString)
     * @return a ResourceBundle matching the passed in param
     */
    public static final ResourceBundle getXMLResourceBundle(final String schemaname) {
        return ResourceManager.getXMLResourceBundle(schemaname);
    }

	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user in the form of a yes or no
	 * question.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display
	 * @return an integer indicating a yes or a no response from the user
	 */
	public static final DialogOption showYesNoMessage(
			final String message, final DialogOption defaultOption) {
		return MessageService.instance().showYesNoMessage(
			message, defaultOption);
	}
    
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user in the form of a yes or no
	 * question.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display
	 * @param defaultValue the IntSetting to store/retrieve the default value
	 * @return an integer indicating a yes or a no response from the user
	 */
	public static final DialogOption showYesNoMessage(
			final String message,
			final IntSetting defaultValue,
			final DialogOption defaultOption) {
		return MessageService.instance().showYesNoMessage(
			message, defaultValue, defaultOption);
	}
    
    public static final DialogOption showYesNoTitledMessage(
            final String message,
            final String title,
            final DialogOption defaultOption) {
		return MessageService.instance().showYesNoMessage(
                message, title, defaultOption);
	}
	
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user. Below a non-selectable list is 
	 * shown. This is in the form of a yes or no or cancel question.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display
	 * @param listModel the array of object to be displayed in the list
	 * @param messageType either {@link JOptionPane#YES_NO_OPTION}, 
	 * {@link JOptionPane#YES_NO_CANCEL_OPTION} or {@link JOptionPane#OK_CANCEL_OPTION}.
	 * @param listRenderer optional list cell rendere, can be <code>null</code>
	 * 
	 * @return an integer indicating a yes or a no or cancel response 
	 * from the user, see {@link JOptionPane#showConfirmDialog(Component, Object, String, int)}
	 */
	public static final int showConfirmListMessage(final String message,
			final Object[] listModel, int messageType, final ListCellRenderer listRenderer) {
		return MessageService.instance().showConfirmListMessage(
				message, listModel, messageType, listRenderer);
	}
    
    /**
     * Displays a locale-specific message to the user in the form of a
     * yes/no/{other} question.<p>
     *
     * The <tt>messageKey</tt> parameter must be the key for a locale-
     * specific message <tt>String</tt> and not a hard-coded value.
     *
     * @param message the locale-specific message to display
     * @param defaultValue the IntSetting to store/retrieve the default value
     * @param otherOptions the name of the other option
     * @return an integer indicating a yes or a no response from the user
     */
    public static final DialogOption showYesNoOtherMessage(
            final String message, final IntSetting defaultValue, String otherOptions) {
        return MessageService.instance().showYesNoOtherMessage(
            message, defaultValue, otherOptions);
    }
    
	
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user in the form of a yes or no or cancel
	 * question.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display
	 * @return an integer indicating a yes or a no response from the user
	 */
	public static final DialogOption showYesNoCancelMessage(
			final String message) {
		return MessageService.instance().showYesNoCancelMessage(
			message);
	}
	
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user in the form of a yes or no or cancel
	 * question.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message for the locale-specific message to display
	 * @param defaultValue the IntSetting to store/retrieve the default value
	 * @return an integer indicating a yes or a no response from the user
	 */
	public static final DialogOption showYesNoCancelMessage(
			final String message, final IntSetting defaultValue) {
		return MessageService.instance().showYesNoCancelMessage(
			message, defaultValue);
	}
	
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param messageKey the key for the locale-specific message to display
	 */
	public static final void showMessage(
			final String messageKey) {
		MessageService.instance().showMessage(messageKey);
	}

	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display
	 * @param ignore the BooleanSetting that stores/retrieves whether or
	 *  not to display this message.
	 */
	public static final void showMessage(
			final String message, final Switch ignore) {
		MessageService.instance().showMessage(message, ignore);
	}

    /**
     * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
     * locale-specific disposable message to the user.<p>
     *
     * The <tt>messageKey</tt> parameter must be the key for a locale-
     * specific message <tt>String</tt> and not a hard-coded value.
     *
     * @param messageKey the key for the locale-specific message to display
     * @param ignore the BooleanSetting that stores/retrieves whether or
     *  not to display this message.
     * @param msgType The <tt>JOptionPane</tt> message type. @see javax.swing.JOptionPane.
     * @param msgTitle The title of the message window.
     */
    public static final void showDisposableMessage(
            final String messageKey, 
            final String message,
            final Switch ignore, 
            int msgType) {
        MessageService.instance().showDisposableMessage(
            messageKey, message, ignore, msgType);
    }
    
    /**
     * Acts as a proxy for the <tt>MessageService</tt> class.  Hides a
     * locale-specific disposable message.<p>
     *
     * The <tt>messageKey</tt> parameter must be the key for a locale-
     * specific message <tt>String</tt> and not a hard-coded value.
     *
     * @param messageKey the key for the locale-specific message to display
     */
    public static final void hideDisposableMessage(
            final String messageKey) {
        MessageService.instance().hideDisposableMessage(
            messageKey);
    }
    
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
	 * confirmation message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display
	 */
	public static final void showConfirmMessage(
			final String message) {
		MessageService.instance().showConfirmMessage(
			message);
	}

	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class. Displays a
	 * confirmation message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display
     * @param ignore the BooleanSetting for that stores/retrieves whether
     *  or not to display this message.
	 */
	public static final void showConfirmMessage(
			final String message,
			final Switch ignore) {
		MessageService.instance().showConfirmMessage(
			message, ignore);
	}
    
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display.
	 */
	public static final void showError(
			final String message) {
		closeStartupDialogs();
		MessageService.instance().showError(message);
	}

	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the key for the locale-specific message to display.
     * @param ignore the BooleanSetting for that stores/retrieves whether
     *  or not to display this message.
	 */
	public static final void showError(
			final String message,
			final Switch ignore) {
        closeStartupDialogs();
		MessageService.instance().showError(
			message, ignore);
	}
	
	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific warning message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display.
     * @param ignore the BooleanSetting for that stores/retrieves whether
     *  or not to display this message.
	 */
	public static final void showWarning(
			final String message,
			final Switch ignore) {

        closeStartupDialogs();
		MessageService.instance().showWarning(
			message, ignore);
	}

	/**
	 * Acts as a proxy for the <tt>MessageService</tt> class.  Displays a
	 * locale-specific warning message to the user.<p>
	 *
	 * The <tt>messageKey</tt> parameter must be the key for a locale-
	 * specific message <tt>String</tt> and not a hard-coded value.
	 *
	 * @param message the locale-specific message to display.
	 */
	public static final void showWarning(final String message) {
        closeStartupDialogs();
		MessageService.instance().showWarning(message);
	}

	/**
	 * Acts as a proxy for the Launcher class so that other classes only need
	 * to know about this mediator class.
	 *
	 * <p>Opens the specified url in a browser.
	 *
	 * @param url the url to open
	 * @return an int indicating the success of the browser launch
	 */
	public static final int openURL(String url) {
	    try {
		    return Launcher.openURL(url);
        } catch(IOException ioe) {
            GUIMediator.showError(I18n.tr("LimeWire could not locate your web browser to display the following webpage: {0}.", url));
            return -1;
        }
	}

	/**
	 * Acts as a proxy for the Launcher class so that other classes only need
	 * to know about this mediator class.
	 *
	 * <p>Launches the file specified in its associated application.
	 *
	 * @param file a <tt>File</tt> instance denoting the abstract pathname
	 *             of the file to launch
	 * @throws IOException if the file cannot be launched do to an IO problem
	 */
	public static final void launchFile(File file) {
		try {
			Launcher.launchFile(file);
		} catch (SecurityException se) {
			showError(I18n.tr("LimeWire will not launch the specified file for security reasons."));
		} catch (LaunchException e) {
			GUIMediator.showError(I18n.tr("LimeWire could not launch the specified file.\n\nExecuted command: {0}.", 
					StringUtils.explode(e.getCommand(), " ")));
		} catch (IOException e) {
			showError(I18n.tr("LimeWire could not launch the specified file."));
		}
	}

	/**
	 * Acts as a proxy for the Launcher class so that other classes only need
	 * to know about this mediator class.
	 *
	 * <p>Opens <tt>file</tt> in a platform specific file manager.
	 *
	 * @param file a <tt>File</tt> instance denoting the abstract pathname
	 *             of the file to launch
	 * @throws IOException if the file cannot be launched do to an IO problem
	 */
	public static final void launchExplorer(File file) {
		try {
			Launcher.launchExplorer(file);
		} catch (SecurityException e) {
			showError(I18n.tr("LimeWire will not launch the specified file for security reasons."));
		} catch (LaunchException e) {
			GUIMediator.showError(I18n.tr("LimeWire could not launch the specified file.\n\nExecuted command: {0}.", 
					StringUtils.explode(e.getCommand(), " ")));
		} catch (IOException e) {
			showError(I18n.tr("LimeWire could not launch the specified file."));
		}
	}

	/**
	 * Returns a <tt>Component</tt> standardly sized for horizontal separators.
	 *
	 * @return the constant <tt>Component</tt> used as a standard horizontal
	 *         separator
	 */
	public static final Component getHorizontalSeparator() {
		return Box.createRigidArea(new Dimension(6,0));
	}

	/**
	 * Returns a <tt>Component</tt> standardly sized for vertical separators.
	 *
	 * @return the constant <tt>Component</tt> used as a standard vertical
	 *         separator
	 */
	public static final Component getVerticalSeparator() {
		return Box.createRigidArea(new Dimension(0,6));
	}

	/**
	 * Connects the user from the network.
	 */
	public void connect() {
	    GuiCoreMediator.getConnectionServices().connect();
		GuiCoreMediator.getDHTManager().setEnabled(true);
	}

	/**
	 * Disconnects the user to the network.
	 */
	public void disconnect() {
	    GuiCoreMediator.getConnectionServices().disconnect();
		GuiCoreMediator.getDHTManager().setEnabled(false);
	}
    
    /**
     * Notifies the user that LimeWire is disconnected
     */
	public static void disconnected() {
        showDisposableMessage(
                DISCONNECTED_MESSAGE,
                I18n.tr("Your machine does not appear to have an active Internet connection or a firewall is blocking LimeWire from accessing the internet. LimeWire will automatically keep trying to connect you to the network unless you select \"Disconnect\" from the File menu."),
                QuestionsHandler.NO_INTERNET_RETRYING,
                JOptionPane.ERROR_MESSAGE);
    }
    
	/**
	 * Returns a <tt>boolean</tt> specifying whether or not the user has
	 * donated to the LimeWire project.
	 *
	 * @return <tt>true</tt> if the user has donated, <tt>false</tt> otherwise
	 */
	public static boolean hasDonated() {
		return HAS_DONATED;
	}

	/**
	 * Sets the visible/invisible state of the tab.
	 *
	 * @param tab the tab to make visible or invisible
	 * @param visible the visible/invisible state to set the tab to
	 */
	public void setTabVisible(GUIMediator.Tabs tab, boolean visible) {
		MAIN_FRAME.setTabVisible(tab, visible);
	}

	/**
	 * Modifies the text displayed to the user in the splash screen to
	 * provide application loading information.
	 *
	 * @param text the text to display
	 */
	public static void setSplashScreenString(String text) {
	    if(!_allowVisible)
		    SplashWindow.instance().setStatusText(text);
        else if(isConstructed())
            instance().STATUS_LINE.setStatusText(text);
	}

	/**
	 * Returns the point for the placing the specified component on the
	 * center of the screen.
	 *
	 * @param comp the <tt>Component</tt> to use for getting the relative
	 *             center point
	 * @return the <tt>Point</tt> for centering the specified
	 *         <tt>Component</tt> on the screen
	 */
	public static Point getScreenCenterPoint(Component comp) {
		final Dimension COMPONENT_DIMENSION = comp.getSize();
		Dimension screenSize =
			Toolkit.getDefaultToolkit().getScreenSize();
		int appWidth = Math.min(screenSize.width,
		                        COMPONENT_DIMENSION.width);
		// compare against a little bit less than the screen size,
		// as the screen size includes the taskbar
		int appHeight = Math.min(screenSize.height - 40,
		                         COMPONENT_DIMENSION.height);
		return new Point((screenSize.width - appWidth) / 2,
		                 (screenSize.height - appHeight) / 2);
	}

	/**
	 * Sets the searching or not searching status of the application.
	 *
	 * @param searching the searching status of the application
	 */
	public void setSearching(boolean searching) {
		MAIN_FRAME.setSearching(searching);
	}

	/**
	 * Adds the specified <tt>RefreshListener</tt> instance to the list of
	 * listeners to be notified when a UI refresh event occurs.
	 *
	 * @param the new <tt>RefreshListener</tt> to add
	 */
	public static void addRefreshListener(RefreshListener listener) {
		if (!REFRESH_LIST.contains(listener))
			REFRESH_LIST.add(listener);
	}

	/**
	 * Removes the specified <tt>RefreshListener</tt> instance from the list
	 * of listeners to be notified when a UI refresh event occurs.
	 * 
	 * @param the <tt>RefreshListener</tt> to remove
	 */
	public static void removeRefreshListener(RefreshListener listener) {
		REFRESH_LIST.remove(listener);
	}
	
	/**
	 * Returns the <tt>Locale</tt> instance currently in use.
	 *
	 * @return the <tt>Locale</tt> instance currently in use
	 */
	public static Locale getLocale() {
		return ResourceManager.getLocale();
	}

	/**
     * Returns true if the current locale is English.
     */
    public static boolean isEnglishLocale() {
        return LanguageUtils.isEnglishLocale(getLocale());
    }

    /**
     * Launches the specified audio song in the player. Adds this song
     * to the play list and begins playing it. 
     *
     * @param song the <tt>PlayListItem</tt> instance to launch
     */
    public void launchAudio(PlayListItem song) {
        launchAudio(song, true);
    }
    
    /**
     * Launches the specified audio song in the player. 
     * @param song  - song to play now
     * @param enqueueSong - if true, also add this song to the playlist, otherwise just
     *          
     */
    public void launchAudio(PlayListItem song, boolean addSongToPlaylist) {       
        if( addSongToPlaylist ) {
            PlaylistMediator playList = PlaylistMediator.getInstance();
            playList.add(song, playList.getSize());
            playList.setSelectedIndex(song);
        } 

        MediaPlayerComponent mediaPlayer = MediaPlayerComponent.getInstance();
        mediaPlayer.loadSong(song, !addSongToPlaylist);
    }

    /**
     * Makes the update message show up in the status panel
     */
    public void showUpdateNotification(final UpdateInformation info) {
        safeInvokeAndWait(new Runnable() {
            public void run() {
                STATUS_LINE.showUpdatePanel(true, info);
            }
        });
    }

   /**
    * Trigger a search based on a string.
    *
    * @param query the query <tt>String</tt>
    * @return the GUID of the query sent to the network.
    *         Used mainly for testing
    */
   public byte[] triggerSearch(String query) {
       MAIN_FRAME.setSelectedTab(GUIMediator.Tabs.SEARCH);
       return SearchMediator.triggerSearch(query);
    }
    
    /**
     * Notification that the button state has changed.
     */
    public void buttonViewChanged() {
        IconManager.instance().wipeButtonIconCache();
        updateButtonView(FRAME);
    }
    
    private void updateButtonView(Component c) {
        if (c instanceof IconButton) {
            ((IconButton) c).updateUI();
        }
        Component[] children = null;
        if (c instanceof Container) {
            children = ((Container)c).getComponents();
        }
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                updateButtonView(children[i]);
            }
        }
    }
   
    /**
     * trigger a browse host based on address and port
     */
    public void doBrowseHost(Connectable host) {
        MAIN_FRAME.setSelectedTab(GUIMediator.Tabs.SEARCH);
        SearchMediator.doBrowseHost(host, null);
    }

    /**
     * safely run code synchronously in the event dispatching thread.
     */
    public static void safeInvokeAndWait(Runnable runnable) {
        if (EventQueue.isDispatchThread())
            runnable.run();
        else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InvocationTargetException ite) {
                Throwable t = ite.getTargetException();
                if(t instanceof Error)
                    throw (Error)t;
                else if(t instanceof RuntimeException)
                    throw (RuntimeException)t;
                else
                    ErrorService.error(t);
            } catch(InterruptedException ignored) {}
        }
    }
	
	/**
	 * InvokesLater if not already in the dispatch thread.
	 */
	public static void safeInvokeLater(Runnable runnable) {
		if (EventQueue.isDispatchThread())
			runnable.run();
		else
			SwingUtilities.invokeLater(runnable);
	}

	/**
	 * Changes whether the media player is enabled and updates the GUI accordingly. 
	 */
	public void setPlayerEnabled(boolean value) {
		if (value == PlayerSettings.PLAYER_ENABLED.getValue())
			return;
		PlayerSettings.PLAYER_ENABLED.setValue(value);
		getStatusLine().refresh();
		LIBRARY_MEDIATOR.setPlayerEnabled(value);
		LibraryPlayListTab.setPlayerEnabled(value);
	}
	
	/**
	 * Sets the cursor on limewire's frame.
	 * @param cursor the cursor that should be shown on the frame and all its
	 * child components that don't have their own cursor set
	 */
	public void setFrameCursor(Cursor cursor) {
		FRAME.setCursor(cursor);
	}
    
}

