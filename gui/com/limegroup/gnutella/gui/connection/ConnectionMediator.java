package com.limegroup.gnutella.gui.connection;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager.ConnectType;

import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.gui.AutoCompleteTextField;
import com.limegroup.gnutella.gui.ClearableAutoCompleteTextField;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.WholeNumberField;
import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.tables.TableSettings;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;

/**
 * This class acts as a mediator between all of the components of the
 * connection window.
 */
public final class ConnectionMediator extends AbstractTableMediator<ConnectionModel, ConnectionDataLine, RoutedConnection> {

    public static ConnectionMediator instance() { return INSTANCE; }

    /**
     * Listeners so buttons and possibly future right-click menu share.
     */
    ActionListener ADD_LISTENER;

    /**
     * Listeners so buttons and possibly future right-click menu share.
     */
    ActionListener BROWSE_HOST_LISTENER;

    private static final String IS_ULTRAPEER =
        I18n.tr("Ultrapeer");

    private static final String IS_LEAF =
        I18n.tr("Leaf");
        
    private static final String CONNECTING = 
        I18n.tr("Connecting");
        
	private static final String LEAVES =
        I18n.tr("Leaves");
        
    private static final String ULTRAPEERS =
        I18n.tr("Ultrapeers");
        
    private static final String PEERS =
        I18n.tr("Peers");

    private static final String STANDARD =
        I18n.tr("Standard");

    /**
     * Instance of singleton access
     */
    private static final ConnectionMediator INSTANCE =
        new ConnectionMediator();

    /**
     * Extra component constants
     */
    private JLabel SERVENT_STATUS;
    
    /**
     * The label displaying the number of ultrapeers, peers & leaves.
     */
    private JLabel NEIGHBORS;

    /**
     * Build the listeners
     */
    @Override
    protected void buildListeners() {
        super.buildListeners();
        ADD_LISTENER = new AddListener();
        BROWSE_HOST_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doBrowseHost();
            }
	    };
    }
    
    /**
     * Overriden to have different default values for tooltips.
     */
    @Override
    protected void buildSettings() {
        SETTINGS = new TableSettings(ID) {
            @Override
            public boolean getDefaultTooltips() {
                return false;
            }
        };
    }

    /**
     * Add the listeners
     */
    @Override
    protected void addListeners() {
        super.addListeners();
    }

	/**
	 * Set up the necessary constants.
	 */
	@Override
    protected void setupConstants() {
	    //  Create padded panel without bottom padding so that button
	    //  rows for all the tabs line up.
		MAIN_PANEL = new PaddedPanel();
		DATA_MODEL = new ConnectionModel();
		TABLE = new LimeJTable(DATA_MODEL);
		BUTTON_ROW = (new ConnectionButtons(this)).getComponent();

		SERVENT_STATUS = new JLabel("");
		NEIGHBORS = new JLabel("");
    }
	
	@Override
	protected void setupDragAndDrop() {
		TABLE.setTransferHandler(DNDUtils.DEFAULT_TRANSFER_HANDLER);
	}

    /**
     * Overridden to set the size.
     */
    @Override
    protected JComponent getScrolledTablePane() {
		JComponent pane = super.getScrolledTablePane();

        SCROLL_PANE.setPreferredSize(new Dimension(3000, 5000));

        return pane;
    }

    /**
     * Update the splash screen
     */
	@Override
    protected void updateSplashScreen() {
		GUIMediator.setSplashScreenString(
            I18n.tr("Loading Connections Window..."));
    }

    /**
     * Override the default main panel setup
     */
    @Override
    protected void setupMainPanel() {
        if (MAIN_PANEL == null)
            return;

        super.setupMainPanel();

	    JPanel status = new JPanel();
	    status.setLayout(new BorderLayout());
	    status.add(SERVENT_STATUS, BorderLayout.WEST);
	    status.add(NEIGHBORS, BorderLayout.EAST);

        MAIN_PANEL.add(status, 0);
    }

	/**
	 * Constructor -- private for Singleton access
	 */
	private ConnectionMediator() {
	    super("CONNECTION_TABLE");
	    GUIMediator.addRefreshListener(this);
	    ThemeMediator.addThemeObserver(this);
	    doRefresh();
	}

    /**
     * Removes all selected rows from Router,
     * which will in turn remove it from the list.
     * Overrides default removeSelection
     */
    @Override
    public void removeSelection() {
		int[] sel = TABLE.getSelectedRows();
		Arrays.sort(sel);
		RoutedConnection c;
		for( int counter = sel.length - 1; counter >= 0; counter--) {
			int i = sel[counter];
			c = DATA_MODEL.get(i).getInitializeObject();
			GuiCoreMediator.getConnectionServices().removeConnection(c);
		}
		clearSelection();
    }

    /** 
     * Returns the JPopupMenu for the connection table
     */
    @Override
    protected JPopupMenu createPopupMenu() {
        JPopupMenu jpm = new JPopupMenu();

        //  add
        JMenuItem jmi = new JMenuItem(I18n
                .tr("Add..."));
        jmi.addActionListener(ADD_LISTENER);
        jpm.add(jmi);
        jpm.addSeparator();

        //  remove
        jmi = new JMenuItem(I18n.tr("Remove"));
        jmi.addActionListener(REMOVE_LISTENER);
        jpm.add(jmi);
        jpm.addSeparator();

        //  browse host
        jmi = new JMenuItem(I18n
                .tr("Browse Host"));
        jmi.addActionListener(BROWSE_HOST_LISTENER);
        jpm.add(jmi);

        return jpm;
    }

	/**
	 * Handles the selection of the specified row in the connection window,
	 * enabling or disabling buttons
	 *
	 * @param row the selected row
	 */
	public void handleSelection(int row) {
	    setButtonEnabled( ConnectionButtons.REMOVE_BUTTON, true );
	    setButtonEnabled( ConnectionButtons.BROWSE_HOST_BUTTON, true );
	}

	/**
	 * Handles the deselection of all rows in the download table,
	 * disabling all necessary buttons and menu items.
	 */
	public void handleNoSelection() {
	    setButtonEnabled( ConnectionButtons.REMOVE_BUTTON, false );
	    setButtonEnabled( ConnectionButtons.BROWSE_HOST_BUTTON, false );
	}

    public void handleActionKey() {
        doBrowseHost(); 
    }

    /**
     * get the first selected row and trigger a browse host
     */
    private void doBrowseHost() {
        int[] rows = TABLE.getSelectedRows();
        if(rows.length > 0) {
            RoutedConnection c = DATA_MODEL.get(rows[0]).getInitializeObject();
            GUIMediator.instance().doBrowseHost(c);
        }
    }

	/**
	 * Override the default doRefresh so we can update the servent status label
	 * (Uses doRefresh instead of refresh so this will only get called
	 *  when the table is showing.  Small optimization.)
	 */
	@Override
    public void doRefresh() {
	    super.doRefresh();
	    SERVENT_STATUS.setText(I18n.tr("Connection Status:") + "  " +
	        ( GuiCoreMediator.getConnectionServices().isSupernode() ?
                IS_ULTRAPEER : IS_LEAF ) + "      "
        );
        int[] counts = DATA_MODEL.getConnectionInfo();
        NEIGHBORS.setText("(" +
            counts[1] + " " + ULTRAPEERS + ", " +
            counts[2] + " " + PEERS + ", " + 
            counts[3] + " " + LEAVES + ", " +
            counts[0] + " " + CONNECTING + ", " +
            counts[4] + " " + STANDARD + ")");
    }
    
    /**
     * Determines the number of connections that are in connecting state.
     */
    public int getConnectingCount() {
        return DATA_MODEL.getConnectingCount();
    }

    private void tryConnection(final String hostname, final int portnum, final ConnectType type) {
        BackgroundExecutorService.schedule(new Runnable() {
            public void run() {
                GuiCoreMediator.getConnectionServices().connectToHostAsynchronously(hostname, portnum, type);
            }
        });
    }

    /**
     *  Clear the connections visually
     */
    public void clearConnections() {
		DATA_MODEL.clear();
    }

    /**
     * Adds the host & port to the dictionary of the HOST_INPUT
     */
    void addKnownHost( String host, int port ) {
	    //HOST_INPUT.addToDictionary( host + ":" + port );
	}

    /**
     * First attempts to parse out the ':' from the host.
     * If one exists, it replaces the text in PORT_INPUT.
     * Otherwise, it uses the text in port input.
     */
    private final class AddListener implements ActionListener {
    
    	private JDialog dialog = null;
    
    	private AutoCompleteTextField HOST_INPUT = new ClearableAutoCompleteTextField(20);
    
    	private JTextField PORT_INPUT = new WholeNumberField(6346, 4);
    
    	private JButton OK_BUTTON = new JButton(I18n.tr("OK"));
    
    	private JButton CANCEL_BUTTON = new JButton(I18n.tr("Cancel"));
        
        private JCheckBox TLS_BOX = new JCheckBox();
    
    	private void createDialog() {
    	    if(dialog != null) return;
    	    //  1.  create modal dialog
    	    //      Host: [            ]
    	    //      Port: [            ]
            //      TLS:  [X]
    	    //         [ OK ] [ Cancel ]
    	    dialog = new JDialog(GUIMediator.getAppFrame(),
    				 I18n.tr("Add Gnutella Connection"),
    				 true);
    	    JPanel jp = (JPanel)dialog.getContentPane();
    	    GUIUtils.addHideAction(jp);
    	    jp.setLayout(new GridBagLayout());
    	    GridBagConstraints gbc = new GridBagConstraints();
            
            int spacing = 6;
            
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.anchor = GridBagConstraints.WEST;
            
    	    //  host label
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(spacing, spacing, 0, 0);
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            jp.add(new JLabel(I18n.tr("Host:")), gbc);
            
    	    //  host input field
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(spacing, spacing, 0, spacing);
    	    jp.add(HOST_INPUT, gbc);
    
    	    //  port label
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.insets = new Insets(spacing, spacing, 0, 0);
    	    jp.add(new JLabel(I18n.tr("Port:")), gbc);
    	    
    	    //  port input field
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(spacing, spacing, 0, spacing);
            jp.add(PORT_INPUT, gbc);

            //  TLS label
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.insets = new Insets(spacing, spacing, 0, 0);
            jp.add(new JLabel(I18n.tr("Use TLS:")), gbc);
            
            //  TLS checkbox
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(spacing, spacing, 0, spacing);
            jp.add(TLS_BOX, gbc);
            
    	    //  buttons
    	    JPanel buttons = new JPanel();
    	    OK_BUTTON.addActionListener(new ActionListener() {
    		    public void actionPerformed(ActionEvent ae) {
    			String hostnamestr = HOST_INPUT.getText();
    			String portstr = PORT_INPUT.getText();
    
    			// look for the port in the host
    			int idx = hostnamestr.lastIndexOf(':');
    			// if it exists, rewrite the host & port
    			if ( idx != -1 ) {
    			    PORT_INPUT.setText( hostnamestr.substring(idx+1) );
    			    portstr = PORT_INPUT.getText();
    			    HOST_INPUT.setText( hostnamestr.substring(0, idx) );
    			    hostnamestr = HOST_INPUT.getText();
    			}
    
    			int portnum = -1;
    			try {
    			    portnum = Integer.parseInt(portstr);
    			} catch (NumberFormatException ee) {
    			    portnum = 6346;
    			}
    			if(!NetworkUtils.isValidPort(portnum))
    			    portnum = 6346;
                
    			PORT_INPUT.setText("" + portnum);
    
    			if ( !hostnamestr.equals("") ) {
    			    tryConnection(hostnamestr, portnum, TLS_BOX.isSelected() ? ConnectType.TLS : ConnectType.PLAIN);
    			    dialog.setVisible(false);
    			    dialog.dispose();
    			} else {
    			    HOST_INPUT.requestFocus();
    			}
    		    }
    		});
    	    CANCEL_BUTTON.addActionListener(GUIUtils.getDisposeAction());
    	    buttons.add(OK_BUTTON);
    	    buttons.add(CANCEL_BUTTON);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(spacing, spacing, spacing, spacing);
            gbc.weightx = 1;
            gbc.weighty = 1;
    	    jp.add(buttons, gbc);
       	}

        public void actionPerformed(ActionEvent e) {
    	    if(dialog == null) createDialog();
    
    	    //  2.  display dialog centered (and modal)
    	    dialog.getRootPane().setDefaultButton(OK_BUTTON);
    	    dialog.pack();
    	    GUIUtils.centerOnScreen(dialog);
    	    dialog.setVisible(true);
    	}
    }
}
