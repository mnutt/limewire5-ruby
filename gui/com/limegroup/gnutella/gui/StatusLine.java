package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.PlayerSettings;
import org.limewire.core.settings.StatusBarSettings;
import org.limewire.setting.BooleanSetting;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.gui.mp3.MediaPlayerComponent;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * The component for the space at the bottom of the main application
 * window, including the connected status and the media player.
 */
public final class StatusLine implements ThemeObserver {

	/**
     * The different connection status possibilities.
     */
    public static final int STATUS_DISCONNECTED = 0;
    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_POOR = 2;
    public static final int STATUS_FAIR = 3;
    public static final int STATUS_GOOD = 4;
    public static final int STATUS_EXCELLENT = 5;
    public static final int STATUS_TURBOCHARGED = 6;
    public static final int STATUS_IDLE = 7;
    public static final int STATUS_WAKING_UP = 8;

    /**
     * The main container for the status line component.
     */
    private final JPanel BAR = new JPanel(new GridBagLayout());
    
    /**
     * The left most panel containing the connection quality.
     * The switcher changes the actual ImageIcons on this panel.
     */
    private final JLabel _connectionQualityMeter = new JLabel();
    private final ImageIcon[] _connectionQualityMeterIcons = new ImageIcon[9];

    /**
     * The button for the current language flag to allow language switching
     */
    private final LanguageButton _languageButton = new LanguageButton();
    
    /**
     * The label with the firewall status.
     */
    private final JLabel _firewallStatus = new JLabel();
	
    /**
     * The custom component for displaying the number of shared files.
     */
    private final SharedFilesLabel _sharedFiles = new SharedFilesLabel();
    
	/**
     * The label with the store status.
     */
    private final JLabel _lwsStatus = new JLabel();     
    
	/**
	 * The labels for displaying the bandwidth usage.
	 */
	private final JLabel _bandwidthUsageDown = new LazyTooltip(GUIMediator.getThemeImage("downloading_small")); 
	private final JLabel _bandwidthUsageUp = new LazyTooltip(GUIMediator.getThemeImage("uploading_small")); 
    
    /**
     * Variables for the center portion of the status bar, which can display
     * the StatusComponent (progress bar during program load), the UpdatePanel
     * (notification that a new version of LimeWire is available), and the
     * StatusLinkHandler (ads for going PRO).
     */
    private final StatusComponent STATUS_COMPONENT = new StatusComponent();
    private final UpdatePanel _updatePanel = new UpdatePanel();
	private final StatusLinkHandler _statusLinkHandler = new StatusLinkHandler();
	private final JPanel _centerPanel = new JPanel(new GridBagLayout());
	private Component _centerComponent = _updatePanel;

    /**
     * The media player.
     */
    private MediaPlayerComponent _mediaPlayer;
    
    private final NetworkManager networkManager;

    
    ///////////////////////////////////////////////////////////////////////////
    //  Construction
    ///////////////////////////////////////////////////////////////////////////
        
    /**
     * Creates a new status line in the disconnected state.
     */
    public StatusLine(NetworkManager networkManager) {
        this.networkManager = networkManager;
        
        GUIMediator.setSplashScreenString(
            I18n.tr("Loading Status Window..."));

		GUIMediator.addRefreshListener(REFRESH_LISTENER);
		BAR.addMouseListener(STATUS_BAR_LISTENER);
		GUIMediator.getAppFrame().addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent arg0) { refresh(); }
			public void componentMoved(ComponentEvent arg0) { }
			public void componentShown(ComponentEvent arg0) { }
			public void componentHidden(ComponentEvent arg0) { }
		});
        
		//  make icons and panels for connection quality
        createConnectionQualityPanel();
        
        //  make the 'Language' button
        createLanguageButton();

        //  make the 'Firewall Status' label
        createFirewallLabel();
        
        //  make the 'LWS Status' label
        createLWSLabel();        
        
        //  make the 'Sharing X Files' component
		createSharingFilesLabel();

		//  make the 'Bandwidth Usage' label
		createBandwidthLabel();
		
		//  make the center panel
		createCenterPanel();
		
        // Set the bars to not be connected.
        setConnectionQuality(0);

	    ThemeMediator.addThemeObserver(this);

		refresh();
    }

	/**
	 * Redraws the status bar based on changes to StatusBarSettings,
	 * and makes sure it has room to add an indicator before adding it.
	 */
	public void refresh() {
		BAR.removeAll();
        
		//  figure out remaining width, and do not add indicators if no room
		int sepWidth = Math.max(2, createSeparator().getWidth());
		int remainingWidth = BAR.getWidth();
		if (remainingWidth <= 0)
			remainingWidth = ApplicationSettings.APP_WIDTH.getValue();
		
		//  subtract player as needed
		if (GUIMediator.isPlaylistVisible()) {
			if (_mediaPlayer == null)
                _mediaPlayer = MediaPlayerComponent.getInstance();
			remainingWidth -= sepWidth;
			remainingWidth -= GUIConstants.SEPARATOR / 2;
            remainingWidth -= _mediaPlayer.minWidth;
			remainingWidth -= GUIConstants.SEPARATOR;
		}
		
		//  subtract center component
		int indicatorWidth = _centerComponent.getWidth();
		if (indicatorWidth <= 0)
            if (_updatePanel.shouldBeShown()) {
                indicatorWidth = 190;
			    if (!GUIMediator.hasDonated()) 
                    indicatorWidth = 280;
            }
		remainingWidth -= indicatorWidth;

        //  add components to panel, if room
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0,0,0,0);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = GridBagConstraints.RELATIVE;

        //  add connection quality indicator if there's room
        indicatorWidth = GUIConstants.SEPARATOR +
            Math.max((int)_connectionQualityMeter.getMinimumSize().getWidth(),
                    _connectionQualityMeter.getWidth()) + sepWidth;
        if (StatusBarSettings.CONNECTION_QUALITY_DISPLAY_ENABLED.getValue() &&
                remainingWidth > indicatorWidth) {
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(_connectionQualityMeter, gbc);
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(createSeparator(), gbc);
            remainingWidth -= indicatorWidth;
        }
        
        //  add the language button if there's room
        indicatorWidth = GUIConstants.SEPARATOR +
        	Math.max((int) _languageButton.getMinimumSize().getWidth(),
        			_languageButton.getWidth()) + sepWidth;
        
        BooleanSetting languageSetting = getLanguageSetting();
        if (languageSetting.getValue() && remainingWidth > indicatorWidth) {
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(_languageButton, gbc);
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(createSeparator(), gbc);
            remainingWidth -= indicatorWidth;        
        }
        

        //  then add firewall display if there's room
        indicatorWidth = GUIConstants.SEPARATOR +
            Math.max((int)_firewallStatus.getMinimumSize().getWidth(),
                    _firewallStatus.getWidth()) + sepWidth;
        if (StatusBarSettings.FIREWALL_DISPLAY_ENABLED.getValue() &&
                remainingWidth > indicatorWidth) {
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(_firewallStatus, gbc);
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(createSeparator(), gbc);
            remainingWidth -= indicatorWidth;
        }
        
        //  then add store display if there's room
        indicatorWidth = GUIConstants.SEPARATOR +
            Math.max((int)_lwsStatus.getMinimumSize().getWidth(),
                    _lwsStatus.getWidth()) + sepWidth;
        if (StatusBarSettings.LWS_DISPLAY_ENABLED.getValue() &&
                remainingWidth > indicatorWidth) {
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(_lwsStatus, gbc);
            BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
            BAR.add(createSeparator(), gbc);
            remainingWidth -= indicatorWidth;
        }         
        
		//  add shared files indicator if there's room
		indicatorWidth = GUIConstants.SEPARATOR +
            Math.max((int)_sharedFiles.getMinimumSize().getWidth(),
                    _sharedFiles.getWidth()) + sepWidth;
        if (StatusBarSettings.SHARED_FILES_DISPLAY_ENABLED.getValue() &&
				remainingWidth > indicatorWidth) {
			BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
			BAR.add(_sharedFiles, gbc);
			BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
			BAR.add(createSeparator(), gbc);
			remainingWidth -= indicatorWidth;
        }

		//  add bandwidth display if there's room
		indicatorWidth = GUIConstants.SEPARATOR + GUIConstants.SEPARATOR / 2 + sepWidth +
			Math.max((int)_bandwidthUsageDown.getMinimumSize().getWidth(), _bandwidthUsageDown.getWidth()) +
            Math.max((int)_bandwidthUsageUp.getMinimumSize().getWidth(), _bandwidthUsageUp.getWidth());
        if (StatusBarSettings.BANDWIDTH_DISPLAY_ENABLED.getValue() &&
				remainingWidth > indicatorWidth) {
			BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
			BAR.add(_bandwidthUsageDown, gbc);
			BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR), gbc);
			BAR.add(_bandwidthUsageUp, gbc);
			BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
			BAR.add(createSeparator(), gbc);
			remainingWidth -= indicatorWidth;
        }

		BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
        //  make center panel stretchy
        gbc.weightx = 1;
		BAR.add(_centerPanel, gbc);
        gbc.weightx = 0;
		BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);

        //  media player
        if (GUIMediator.isPlaylistVisible()) {
			JPanel jp = _mediaPlayer.getMediaPanel();
            // if room to display volume and progress, do so
            if(remainingWidth + _mediaPlayer.minWidth > _mediaPlayer.fullSizeWidth ){
                jp.setPreferredSize( new Dimension(_mediaPlayer.fullSizeWidth, 25));
            }
            else {//else just display buttons
                jp.setPreferredSize( new Dimension(_mediaPlayer.minWidth, 25));
            }
            
			BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 2), gbc);
			BAR.add(jp, gbc);
			BAR.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR), gbc);
        }

		BAR.validate();
		BAR.repaint();
	}

	/**
     * Creates a vertical separator for visually separating status bar elements 
     */
    private Component createSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        //  separators need preferred size in GridBagLayout
        sep.setPreferredSize(new Dimension(2, 20));
        sep.setMinimumSize(new Dimension(2, 20));
        return sep;
    }

    /**
     * Sets up _connectionQualityMeter's icons.
     */
    private void createConnectionQualityPanel() {
		updateTheme();  // loads images
		_connectionQualityMeter.setOpaque(false);
        _connectionQualityMeter.setMinimumSize(new Dimension(34, 20));
        _connectionQualityMeter.setMaximumSize(new Dimension(90, 30));
		//   add right-click listener
		_connectionQualityMeter.addMouseListener(STATUS_BAR_LISTENER);
	}

	/**
	 * Sets up the 'Sharing X Files' label.
	 */
	private void createSharingFilesLabel() {
        _sharedFiles.setHorizontalAlignment(SwingConstants.LEFT);
	    // don't allow easy clipping
		_sharedFiles.setMinimumSize(new Dimension(24, 20));
		// add right-click listener
		_sharedFiles.addMouseListener(STATUS_BAR_LISTENER);
        //  initialize tool tip
        _sharedFiles.updateToolTip(0);
	}

    /**
	 * Sets up the 'Language' button
	 */
	private void createLanguageButton() {
		_languageButton.addMouseListener(STATUS_BAR_LISTENER);
		updateLanguage();
	}

	
	/**
	 * Sets up the 'Firewall Status' label.
	 */
	private void createFirewallLabel() {
		updateFirewall();
		// don't allow easy clipping
		_firewallStatus.setMinimumSize(new Dimension(20, 20));
		// add right-click listener
		_firewallStatus.addMouseListener(STATUS_BAR_LISTENER);
	}
	
	/**
     * Sets up the 'Store Status' label.
     */
    private void createLWSLabel() {
        updateLWS();
        // don't allow easy clipping
        _lwsStatus.setMinimumSize(new Dimension(20, 20));
        // add right-click listener
        _lwsStatus.addMouseListener(STATUS_BAR_LISTENER);
    }        
	
	/**
	 * Sets up the 'Bandwidth Usage' label.
	 */
	private void createBandwidthLabel() {
		updateBandwidth();
		// don't allow easy clipping
		_bandwidthUsageDown.setMinimumSize(new Dimension(60, 20));
		_bandwidthUsageUp.setMinimumSize(new Dimension(60, 20));
		// add right-click listeners
		_bandwidthUsageDown.addMouseListener(STATUS_BAR_LISTENER);
		_bandwidthUsageUp.addMouseListener(STATUS_BAR_LISTENER);
	}

	/**
	 * Sets up the center panel.
	 */
	private void createCenterPanel() {
		_centerPanel.setOpaque(false);
        _updatePanel.setOpaque(false);
		((JComponent)_statusLinkHandler.getComponent()).setOpaque(false);
        STATUS_COMPONENT.setProgressPreferredSize(new Dimension(250, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
		_centerPanel.add(STATUS_COMPONENT, gbc);

		//  add right-click listeners
		_statusLinkHandler.getComponent().addMouseListener(STATUS_BAR_LISTENER);
		_centerPanel.addMouseListener(STATUS_BAR_LISTENER);
		_updatePanel.addMouseListener(STATUS_BAR_LISTENER);
		STATUS_COMPONENT.addMouseListener(STATUS_BAR_LISTENER);
	}

	/**
	 * Updates the center panel if non-PRO.  Periodically rotates between
	 * the update panel and the status link handler. 
	 */
	private void updateCenterPanel() {
		long now = System.currentTimeMillis();
		if (_nextUpdateTime > now)
			return;

		_nextUpdateTime = now + 1000 * 5; // update every minute
		_centerPanel.removeAll();
		if (GUIMediator.hasDonated()) {
			if (_updatePanel.shouldBeShown())
				_centerComponent = _updatePanel;
			else
				_centerComponent = new JLabel();
		} else {
			if ((_centerComponent == _statusLinkHandler.getComponent()) && _updatePanel.shouldBeShown())
				_centerComponent = _updatePanel;
			else
				_centerComponent = _statusLinkHandler.getComponent();
		}
		
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        _centerPanel.add(_centerComponent, gbc);
		
		refresh();
	}
	private long _nextUpdateTime = System.currentTimeMillis();

    /**
     * Tells the status linke that the update panel should be shown with
     * the given update information.
     */
    public void showUpdatePanel(boolean popup, UpdateInformation info) {
        _updatePanel.makeVisible(popup, info);
    }
    
    /**
     * Updates the status text.
     */
    void setStatusText(final String text) {
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                STATUS_COMPONENT.setText(text);
            }
        });
    }

	/**
	 * Updates the firewall text. 
	 */
	public void updateFirewallLabel(boolean notFirewalled) {
		if (notFirewalled) {
			_firewallStatus.setIcon(GUIMediator.getThemeImage("firewall_no"));
			_firewallStatus.setToolTipText(I18n.tr("LimeWire has not detected a firewall"));
		} else {
			_firewallStatus.setIcon(GUIMediator.getThemeImage("firewall"));
			_firewallStatus.setToolTipText(I18n.tr("LimeWire has detected a firewall"));
		}
	}

	/**
     * Updates the LimeWire Store text. 
     */
    public void updateLWSLabel(boolean notConnected) {
        if (notConnected) {
            _lwsStatus.setIcon(GUIMediator.getThemeImage("lws_statusline_no"));
            _lwsStatus.setToolTipText(I18n.tr("You are not connected to the LimeWire Store"));
        } else {
            _lwsStatus.setIcon(GUIMediator.getThemeImage("lws_statusline"));
            _lwsStatus.setToolTipText(I18n.tr("You are connected to the LimeWire Store"));
        }
    }      

	/**
	 * Updates the image on the flag
	 */
	public void updateLanguage() {
		_languageButton.updateLanguageFlag();
	}
	
	/**
	 * Updates the firewall text. 
	 */
	public void updateFirewall() {
		updateFirewallLabel(networkManager.acceptedIncomingConnection());
	}
	
	/**
     * Updates the LimeWire Store text. 
     */
    public void updateLWS() {
        updateLWSLabel(true);
    }    
	
	/**
	 * Updates the bandwidth statistics.
	 */
	public void updateBandwidth() {

		//  calculate time-averaged stats
        /*_pastDownloads[_pastBandwidthIndex] = BandwidthStat.HTTP_DOWNSTREAM_BANDWIDTH.getLastStored();
        _pastUploads[_pastBandwidthIndex] = BandwidthStat.HTTP_UPSTREAM_BANDWIDTH.getLastStored();
        _pastBandwidthIndex = (_pastBandwidthIndex + 1) % _numTimeSlices;
        /*int upBW = 0; 
        int downBW = 0;
        for (int i = 0; i < _numTimeSlices; i++)
            downBW += _pastDownloads[i];
        downBW /= _numTimeSlices; 
        for (int i = 0; i < _numTimeSlices; i++)
            upBW += _pastUploads[i];
        upBW /= _numTimeSlices;*/

        //  format strings
        String sDown = GUIUtils.rate2speed(GuiCoreMediator.getDownloadManager().getLastMeasuredBandwidth());
        String sUp = GUIUtils.rate2speed(GuiCoreMediator.getUploadManager().getLastMeasuredBandwidth());
        int downloads = GuiCoreMediator.getDownloadServices().getNumActiveDownloads();
        int uploads = GuiCoreMediator.getUploadServices().getNumUploads();
		_bandwidthUsageDown.setText(downloads + " @ " + sDown);
		_bandwidthUsageUp.setText(uploads +   " @ " + sUp);
	}
	
    /**
     * Notification that loading has finished.
     *
     * The loading label is removed and the update notification
     * component is added.  If necessary, the center panel will
     * rotate back and forth between displaying the update
     * notification and displaying the StatusLinkHandler.
     */
    void loadFinished() {
		updateCenterPanel();
		_centerPanel.revalidate();
        _centerPanel.repaint();
		refresh();
    }

	/**
     * Load connection quality theme icons
	 */
	public void updateTheme() {
        for (int i = 0; i < _connectionQualityMeterIcons.length; i++)
            _connectionQualityMeterIcons[i] = GUIMediator.getThemeImage("connect_small_" + i);
        
		if (_mediaPlayer != null)
			_mediaPlayer.updateTheme();
	}

    /**
     * Alters the displayed connection quality.
     *
     * @modifies this
     */
    public void setConnectionQuality(int quality) {
        // make sure we don't go over our bounds.
        if (quality >= _connectionQualityMeterIcons.length)
            quality = _connectionQualityMeterIcons.length - 1;

        _connectionQualityMeter.setIcon(_connectionQualityMeterIcons[quality]);

        String status = null;
        String tip = null;
        String connection = I18n.tr("Connection");
        switch(quality) {
            case STATUS_DISCONNECTED:
                	status = I18n.tr("Disconnected");
                    tip = I18n.tr("You are disconnected from the network. To connect, choose Connect from the File menu.");
                    break;
            case STATUS_CONNECTING:
                    status = I18n.tr("Starting") + " " + connection;
                    tip = I18n.tr("You are currently connecting to the network");
                    break;
            case STATUS_POOR:
                    status = I18n.tr("Poor") + " " + connection;
                    tip = I18n.tr("You are connected to few hosts and have an unstable connection the network");
                    break;
            case STATUS_FAIR:
                    status = I18n.tr("Fair") + " " + connection;
                    tip = I18n.tr("Your connection to the network is getting stronger");
                    break;
            case STATUS_GOOD:
                    status = I18n.tr("Good") + " " + connection;
                    tip = I18n.tr("You are not yet fully connected to the network, but have a very good connection");
                    break;
            case STATUS_IDLE:
            case STATUS_EXCELLENT:
                    status = I18n.tr("Excellent") + " " + connection;
                    tip = I18n.tr("Your connection to the network is very strong");
                    break;
            case STATUS_TURBOCHARGED:
                    status = I18n.tr("Turbo-Charged") + " " + connection;
                    tip = LimeWireUtils.isPro() ? I18n.tr("Your connection to the network is extremely strong") :
                        I18n.tr("You can experience Turbo-Charged connections all the time with LimeWire PRO!");
                    break;
            //case STATUS_IDLE:
                    //status = STATISTICS_CONNECTION_IDLE;
                    //tip = null; // impossible to see this
                    //break;
            case STATUS_WAKING_UP:
                    status = I18n.tr("Waking Up") + " " + connection;
                    tip = I18n.tr("LimeWire is waking up from sleep mode");
                    break;
        }
        _connectionQualityMeter.setToolTipText(tip);
        if (GUIMediator.hasDonated())
            _connectionQualityMeter.setText(status);
    }

    /**
     * Sets the horizon statistics for this.
     * @modifies this
     * @return A displayable Horizon string.
     */
    public void setStatistics(int share) {
		_sharedFiles.update(share);
    }

    /**
      * Accessor for the <tt>JComponent</tt> instance that contains all
      * of the panels for the status line.
      *
      * @return the <tt>JComponent</tt> instance that contains all
      *  of the panels for the status line
      */
    public JComponent getComponent() {
        return BAR;
    }
	
    /**
     * The refresh listener for updating the bandwidth usage every second.
     */
    private final RefreshListener REFRESH_LISTENER = new RefreshListener() {
        public void refresh() {
            if (StatusBarSettings.BANDWIDTH_DISPLAY_ENABLED.getValue())
                updateBandwidth();
            updateCenterPanel();
        }
    };
    
    private BooleanSetting getLanguageSetting() {
        if (GUIMediator.isEnglishLocale())
            return StatusBarSettings.LANGUAGE_DISPLAY_ENGLISH_ENABLED;
        else
            return StatusBarSettings.LANGUAGE_DISPLAY_ENABLED;
    }
    
    /**
     * The right-click listener for the status bar.
     */
	private final MouseAdapter STATUS_BAR_LISTENER = new MouseAdapter() {
		@Override
        public void mousePressed(MouseEvent me) { processMouseEvent(me); }
		@Override
        public void mouseReleased(MouseEvent me) { processMouseEvent(me); }
		@Override
        public void mouseClicked(MouseEvent me) { processMouseEvent(me); }
		
		public void processMouseEvent(MouseEvent me) {
			if (me.isPopupTrigger()) {
                JPopupMenu jpm = new JPopupMenu();
                
                //  add 'Show Connection Quality' menu item
                JCheckBoxMenuItem jcbmi = new JCheckBoxMenuItem(new ShowConnectionQualityAction());
                jcbmi.setState(StatusBarSettings.CONNECTION_QUALITY_DISPLAY_ENABLED.getValue());
                jpm.add(jcbmi);

                //  add 'Show International Localization' menu item
                jcbmi = new JCheckBoxMenuItem(new ShowLanguageStatusAction());
                jcbmi.setState(getLanguageSetting().getValue());
                jpm.add(jcbmi);

                
                //  add 'Show Firewall Status' menu item
                jcbmi = new JCheckBoxMenuItem(new ShowFirewallStatusAction());
                jcbmi.setState(StatusBarSettings.FIREWALL_DISPLAY_ENABLED.getValue());
                jpm.add(jcbmi);
                
                // add 'Store Indicator' menu item
                jcbmi = new JCheckBoxMenuItem(new ShowStoreIndicatorStatusAction());
                jcbmi.setState(StatusBarSettings.LWS_DISPLAY_ENABLED.getValue());
                jpm.add(jcbmi);
                
                //  add 'Show Shared Files Count' menu item 
                jcbmi = new JCheckBoxMenuItem(new ShowSharedFilesCountAction());
                jcbmi.setState(StatusBarSettings.SHARED_FILES_DISPLAY_ENABLED.getValue());
                jpm.add(jcbmi);
                
                //  add 'Show Bandwidth Consumption' menu item
                jcbmi = new JCheckBoxMenuItem(new ShowBandwidthConsumptionAction());
                jcbmi.setState(StatusBarSettings.BANDWIDTH_DISPLAY_ENABLED.getValue());
                jpm.add(jcbmi);
                
                jpm.addSeparator();
                
                //  add 'Show Media Player' menu item
                jcbmi = new JCheckBoxMenuItem(new ShowMediaPlayerAction());
                jcbmi.setState(PlayerSettings.PLAYER_ENABLED.getValue());
                jpm.add(jcbmi);
                
                jpm.show(me.getComponent(), me.getX(), me.getY());
            }
		}
	};

	/**
	 * Action for the 'Show Connection Quality' menu item. 
	 */
	private class ShowConnectionQualityAction extends AbstractAction {
		
		public ShowConnectionQualityAction() {
			putValue(Action.NAME, I18n.tr
					("Show Connection Quality"));
		}
		
		public void actionPerformed(ActionEvent e) {
			StatusBarSettings.CONNECTION_QUALITY_DISPLAY_ENABLED.invert();
			refresh();
		}
	}
	
	/**
	 * Action for the 'Show Shared Files Count' menu item. 
	 */
	private class ShowSharedFilesCountAction extends AbstractAction {
		
		public ShowSharedFilesCountAction() {
			putValue(Action.NAME, I18n.tr
					("Show Shared Files Count"));
		}
		
		public void actionPerformed(ActionEvent e) {
			StatusBarSettings.SHARED_FILES_DISPLAY_ENABLED.invert();
			refresh();
		}
	}

	/**
	 * Action for the 'Show Firewall Status' menu item. 
	 */
	private class ShowLanguageStatusAction extends AbstractAction {
		
		public ShowLanguageStatusAction() {
			putValue(Action.NAME, I18n.tr
					("Show Language Status"));
		}
		
		public void actionPerformed(ActionEvent e) {
            BooleanSetting setting = getLanguageSetting();
            setting.invert();
            
			StatusBarSettings.LANGUAGE_DISPLAY_ENABLED.setValue(setting.getValue());
            StatusBarSettings.LANGUAGE_DISPLAY_ENGLISH_ENABLED.setValue(setting.getValue());
			refresh();
		}
	}
	
	
	/**
	 * Action for the 'Show Firewall Status' menu item. 
	 */
	private class ShowFirewallStatusAction extends AbstractAction {
		
		public ShowFirewallStatusAction() {
			putValue(Action.NAME, I18n.tr
					("Show Firewall Status"));
		}
		
		public void actionPerformed(ActionEvent e) {
			StatusBarSettings.FIREWALL_DISPLAY_ENABLED.invert();
			refresh();
		}
	}
	
    /**
     * Action for the 'Store Indicator' menu item
     */
    private class ShowStoreIndicatorStatusAction extends AbstractAction {

        public ShowStoreIndicatorStatusAction() {
            putValue(Action.NAME, I18n.tr
                    ("Show Store Indicator"));
        }
        
        public void actionPerformed(ActionEvent e) {
            StatusBarSettings.LWS_DISPLAY_ENABLED.invert();
            refresh();
        }
    }
    
	/**
	 * Action for the 'Show Bandwidth Consumption' menu item. 
	 */
	private class ShowBandwidthConsumptionAction extends AbstractAction {
		
		public ShowBandwidthConsumptionAction() {
			putValue(Action.NAME, I18n.tr
					("Show Bandwidth Consumption"));
		}
		
		public void actionPerformed(ActionEvent e) {
			StatusBarSettings.BANDWIDTH_DISPLAY_ENABLED.invert();
			refresh();
		}
	}
	
	/**
	 * Action for the 'Show Media Player' menu item. 
	 */
	private class ShowMediaPlayerAction extends AbstractAction {
		
		public ShowMediaPlayerAction() {
			putValue(Action.NAME, I18n.tr
					("Show Media Player"));
		}
		
		public void actionPerformed(ActionEvent e) {
			GUIMediator.instance().setPlayerEnabled(!PlayerSettings.PLAYER_ENABLED.getValue());
		}
	}
	
	/**
	 * Custom component for displaying the number of shared files. 
	 */
	private class SharedFilesLabel extends JLabel {

		/**
		 * The height of this icon.
		 */
		private static final int _height = 20;
		
		/**
		 * The width of this icon.
		 */
		private int _width = 26;
		
		private FontMetrics fm = null;

		private String _string = "0...";
		
		private int _share;

		@Override
        public Dimension getMinimumSize() {
			return getPreferredSize();
		}
		
		@Override
        public Dimension getPreferredSize() {
			return new Dimension(_width, _height);
		}
		
		/**
		 * Updates the component with information about the sharing state. 
		 */
		public void update(int share) {
			boolean shareChanged = share != _share;
			
			_share = share;

			//  if no changes, return
			if (!(shareChanged))
				return;
			
			_string = GUIUtils.toLocalizedInteger(_share);
//			if (!GuiCoreMediator.getFileManager().isLoadFinished())
//				_string += "...";
			
			if (fm != null)
				_width = fm.stringWidth(_string) + _height;
			
			revalidate();
			repaint();

			//  update tooltip
			updateToolTip(share);
		}

        private void updateToolTip(int share) {
//            if (GuiCoreMediator.getFileManager().isLoadFinished()) {
//                // {0}: number of shared files
//				setToolTipText(I18n.trn("You are sharing {0} file", "You are sharing {0} files", share, share));
//            } else {
//                // {0}: number of shared files
//                setToolTipText(I18n.trn("You are sharing {0} file (Loading...)", "You are sharing {0} files (Loading...)", share, share));
//            }
        }
		
		/**
		 * Paints the icon, and then paints the number of shared files on top of it.
		 */
		@Override
        protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			
			RenderingHints rh = g2.getRenderingHints();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			if (fm == null)
				fm = g2.getFontMetrics();
			
            //  create string, set background color
//			if (!GuiCoreMediator.getFileManager().isLoadFinished()) {
//                g2.setPaint(new Color(165, 165, 2));
//                if (!_string.endsWith("..."))
//                    _string += "...";
//            } else if (_string.startsWith("0")) {
//                g2.setPaint(ThemeFileHandler.NOT_SHARING_LABEL_COLOR.getValue());
//                if (_string.endsWith("..."))
//                    _string = _string.substring(0, _string.length() - 3);
//            }
//            else {
//                g2.setPaint(new Color(2, 137, 2));
//                if (_string.endsWith("..."))
//                    _string = _string.substring(0, _string.length() - 3);
//            }

            //  figure out size
            int width = fm.stringWidth(_string) + _height; 
            if (width != _width) {
                _width = width;
                revalidate();
            }

			//  draw the round rectangle
			RoundRectangle2D.Float rect
				= new RoundRectangle2D.Float(0, 0, _width-2, _height-2, _height, _height);
			g2.fill(rect);
			
			//  stroke the rectangle
			g2.setColor(Color.black);
			g2.draw(rect);
			
			//  then draw string
			g2.setColor(Color.white);
			g2.drawString(_string, (rect.width - fm.stringWidth(_string)) / 2f,
					(rect.height + fm.getAscent() - fm.getDescent()) / 2f);
			
			g2.setRenderingHints(rh);
		}
	}
	
	private class LazyTooltip extends JLabel {
		LazyTooltip(ImageIcon icon) {
			super(icon);
			ToolTipManager.sharedInstance().registerComponent(this);
		}

	    @Override
		public String getToolTipText() {
			String sDown = GUIUtils.rate2speed(GuiCoreMediator.getDownloadManager().getLastMeasuredBandwidth());
	        String sUp = GUIUtils.rate2speed(GuiCoreMediator.getUploadManager().getLastMeasuredBandwidth());
	        String totalDown = GUIUtils.toUnitbytes(GuiCoreMediator.getTcpBandwidthStatistics().getTotalDownstream() * 1024);
	        String totalUp = GUIUtils.toUnitbytes(GuiCoreMediator.getTcpBandwidthStatistics().getTotalUpstream() * 1024);
	        int downloads = GuiCoreMediator.getDownloadServices().getNumActiveDownloads();
	        int uploads = GuiCoreMediator.getUploadServices().getNumUploads();
			//  create good-looking table tooltip
			StringBuilder tooltip = new StringBuilder(100);
            tooltip.append("<html><table>")
                   .append("<tr><td>")
                   .append(I18n.tr("Downloads:"))
                       .append("</td><td>")
                       .append(downloads)
                       .append("</td><td>@</td><td align=right>")
                       .append(sDown)
                       .append("</td></tr>")
                   .append("<tr><td>")
                   .append(I18n.tr("Uploads:"))
                       .append("</td><td>")
                       .append(uploads)
                       .append("</td><td>@</td><td align=right>")
                       .append(sUp)
                       .append("</td></tr>")
                   .append("<tr><td>")
                       .append(I18n.tr("Total Downstream:"))
                       .append("</td><td>")
                       .append(totalDown)
                       .append("</td></tr>")
                   .append("<tr><td>")
                       .append(I18n.tr("Total Upstream:"))
                       .append("</td><td>")
                       .append(totalUp)
                       .append("</td></tr>")
                   .append("</table></html>");
            return tooltip.toString();
		}
	}
}
