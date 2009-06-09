package org.limewire.ui.swing.advanced.connection;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.FWTStatusReason;
import org.limewire.core.api.connection.FirewallStatus;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.api.connection.FirewallTransferStatus;
import org.limewire.core.api.connection.FirewallTransferStatusEvent;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.listener.EventBean;
import org.limewire.ui.swing.advanced.connection.PopupManager.PopupProvider;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

import com.google.inject.Inject;

/**
 * Display panel for the connection summary. 
 */
public class ConnectionSummaryPanel extends JPanel {

    private static final String IS_ULTRAPEER = I18n.tr("You are an Ultrapeer node");
    private static final String IS_LEAF = I18n.tr("You are a Leaf node");
    private static final String IS_NOT_FIREWALLED = I18n.tr("You are not behind a firewall");
    private static final String IS_FIREWALLED_TRANSFERS = I18n.tr("You are behind a firewall and support firewall transfers");
    private static final String IS_FIREWALLED_NO_TRANSFERS = I18n.tr("You are behind a firewall and do not support firewall transfers");
    private static final String WHY = I18n.tr("why");
    private static final String CONNECTED_TO = I18n.tr("Connected to:");
    private static final String RESOLVE = I18n.tr("Show hostnames of connected peers");
    
    private static final String CONNECTING = I18n.tr("Connecting");
    private static final String LEAVES = I18n.tr("Leaves");
    private static final String PEERS = I18n.tr("Peers");
    private static final String STANDARD = I18n.tr("Standard");
    private static final String ULTRAPEERS = I18n.tr("Ultrapeers");
    
    /** Manager instance for connection data. */
    private final GnutellaConnectionManager gnutellaConnectionManager;
    
    /** Bean instance for firewall status. */
    private final EventBean<FirewallStatusEvent> firewallStatusBean;
    
    /** Bean instance for firewall transfer status. */
    private final EventBean<FirewallTransferStatusEvent> firewallTransferBean;

    /** List of connections. */
    private TransformedList<ConnectionItem, ConnectionItem> connectionList;

    /** Popup manager for transfer status reason. */
    private final PopupManager reasonPopupManager;

    private JLabel nodeLabel = new JLabel();
    private FirewallLabel firewallLabel = new FirewallLabel();
    private JLabel summaryLabel = new JLabel();
    private JTable summaryTable = new JTable();
    private SummaryTableModel summaryTableModel = new SummaryTableModel();
    private JCheckBox resolveCheckBox = new JCheckBox(RESOLVE);

    /**
     * Constructs the ConnectionDetailPanel to display connections details.
     */
    @Inject
    public ConnectionSummaryPanel(GnutellaConnectionManager gnutellaConnectionManager,
            EventBean<FirewallStatusEvent> firewallStatusBean,
            EventBean<FirewallTransferStatusEvent> firewallTransferBean) {
        
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        this.firewallStatusBean = firewallStatusBean;
        this.firewallTransferBean = firewallTransferBean;
        this.reasonPopupManager = new PopupManager(firewallLabel);
        
        setBorder(BorderFactory.createTitledBorder(""));
        setLayout(new MigLayout("insets 0 0 0 0,fill",
            "[left]",                          // col constraints
            "[top][top][bottom][top,fill]"));  // row constraints
        setPreferredSize(new Dimension(120, 120));
        setOpaque(false);
        
        firewallLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        firewallLabel.setOpaque(false);
        firewallLabel.setOpenUrlsNatively(false);
        firewallLabel.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == EventType.ACTIVATED) {
                    Point location = firewallLabel.getPopupLocation();
                    reasonPopupManager.showTimedPopup(firewallLabel, 
                            location.x + 18, location.y + 10);
                }
            }
        });

        summaryLabel.setText(CONNECTED_TO);

        summaryTable.setModel(summaryTableModel);
        summaryTable.setSelectionForeground(Color.BLACK);
        summaryTable.setPreferredSize(new Dimension(120, 120));
        summaryTable.setShowGrid(false);
        summaryTable.setFocusable(false);

        // Set column widths.
        summaryTable.getColumnModel().getColumn(0).setPreferredWidth(24);
        summaryTable.getColumnModel().getColumn(1).setPreferredWidth(96);

        // Install renderer to align summary value.
        summaryTable.getColumnModel().getColumn(0).setCellRenderer(new SummaryCellRenderer());
        
        resolveCheckBox.setContentAreaFilled(false);
        boolean resolve = SwingUiSettings.RESOLVE_CONNECTION_HOSTNAMES.getValue();
        resolveCheckBox.setSelected(resolve);
        resolveCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean resolve = resolveCheckBox.isSelected();
                SwingUiSettings.RESOLVE_CONNECTION_HOSTNAMES.setValue(resolve);
            }
        });

        add(nodeLabel      , "cell 0 0");
        add(firewallLabel  , "cell 0 1, growx 100");
        add(summaryLabel   , "cell 0 2");
        add(summaryTable   , "cell 0 3");
        add(resolveCheckBox, "cell 0 4");
    }

    @Override
    public void setBackground(Color bgColor) {
        super.setBackground(bgColor);
        if (summaryTable != null) {
            summaryTable.setBackground(bgColor);
            summaryTable.setSelectionBackground(bgColor);
        }
    }
    
    /**
     * Initializes the data models in the container.
     */
    public void initData() {
        if (connectionList == null) {
            // Create connection list for Swing.  We wrap the actual list in a
            // Swing list to ensure that all events are fired on the UI thread.
            connectionList = GlazedListsFactory.swingThreadProxyEventList(
                    gnutellaConnectionManager.getConnectionList());

            // Set node description.
            boolean ultrapeer = gnutellaConnectionManager.isUltrapeer();
            nodeLabel.setText(ultrapeer ? IS_ULTRAPEER : IS_LEAF);

            // Set firewall status.
            updateFirewallStatus();
        }
    }
    
    /**
     * Clears the data models in the container.
     */
    public void clearData() {
        if (connectionList != null) {
            connectionList.dispose();
            connectionList = null;
        }
    }

    /**
     * Triggers a refresh of the data being displayed. 
     */
    public void refresh() {
        updateFirewallStatus();
        summaryTableModel.update(connectionList);
    }

    /**
     * Updates the firewall status fields.
     */
    private void updateFirewallStatus() {
        // Get firewall status.
        FirewallStatus firewallStatus = firewallStatusBean.getLastEvent().getData();
        
        if (firewallStatus == FirewallStatus.FIREWALLED) {
            // Get firewall transfer status and reason.
            FirewallTransferStatusEvent event = firewallTransferBean.getLastEvent();
            FirewallTransferStatus transferStatus = event.getData();
            FWTStatusReason transferReason = event.getType();

            // Set firewall status and reason.
            if (transferStatus == FirewallTransferStatus.DOES_NOT_SUPPORT_FWT) {
                firewallLabel.setStatusText(IS_FIREWALLED_NO_TRANSFERS, getReasonText(transferReason));
            } else {
                firewallLabel.setStatusText(IS_FIREWALLED_TRANSFERS, null);
            }
            
        } else {
            // Not firewalled so clear transfer status and reason.
            firewallLabel.setStatusText(IS_NOT_FIREWALLED, null);
        }
    }

    /**
     * Returns the display text for the specified firewall transfer status 
     * reason.
     */
    private String getReasonText(FWTStatusReason reason) {
        switch (reason) {
        case INVALID_EXTERNAL_ADDRESS:
            return I18n.tr("LimeWire has not been able to determine the external IP address of your NAT or firewall");
        case NO_SOLICITED_INCOMING_MESSAGES:
            return I18n.tr("LimeWire has not received any incoming UDP messages");
        case REUSING_STATUS_FROM_PREVIOUS_SESSION:
            return I18n.tr("LimeWire was not able to support firewall transfers in a previous session");
        case PORT_UNSTABLE:
            return I18n.tr("LimeWire is behind a NAT or firewall that assigns a different external port to each connection");
        case UNKNOWN:
        default:
            return I18n.tr("Unknown");
        }
    }

    /**
     * Label to display firewall transfer status and reason.  FirewallLabel
     * displays a hyperlink when the reason is not blank; the reason is 
     * displayed in a popup window when the link is clicked.
     */
    private class FirewallLabel extends HTMLLabel implements PopupProvider {
        private Point popupLocation;
        private String reasonText;
        
        public FirewallLabel() {
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    popupLocation = e.getPoint();
                }
            });
        }

        @Override
        public Component getPopupContent() {
            if ((reasonText != null) && (reasonText.length() > 0)) {
                // Return tooltip component for popup.
                JToolTip toolTip = createToolTip();
                toolTip.setTipText(reasonText);
                return toolTip;
            } else {
                return null;
            }
        }
        
        public Point getPopupLocation() {
            return popupLocation;
        }
        
        public void setStatusText(String statusText, String reasonText) {
            this.reasonText = reasonText;
            
            if ((reasonText != null) && (reasonText.length() > 0)) {
                StringBuilder builder = new StringBuilder();
                builder.append(statusText);
                builder.append(" (<a href=\"#\">").append(WHY).append("</a>)");
                setText(builder.toString());
            } else {
                setText(statusText);
            }
        }
    }
    
    /**
     * Table cell renderer for connection count values in the summary table.
     * Values are right-aligned with a right margin.
     */
    private class SummaryCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component renderer = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (renderer instanceof JLabel) {
                ((JLabel) renderer).setHorizontalAlignment(JLabel.RIGHT);
                ((JLabel) renderer).setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
            }

            return renderer;
        }
    }
    
    /**
     * Table model for the connection summary table. 
     */
    private class SummaryTableModel extends AbstractTableModel {

        /** A 5-element array containing the number of connections with the
         *  following states: connecting, ultrapeer, peer, leaf, standard.
         */
        private int[] connectCounts = new int[5];

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return 5;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                // Return summary counts.  This maps the table rows to the 
                // elements in the connectCounts array.  Note that table row 3 
                // for 'connecting' maps to array index 0.
                switch (rowIndex) {
                case 0:
                    return connectCounts[1];
                case 1:
                    return connectCounts[2];
                case 2:
                    return connectCounts[3];
                case 3:
                    return connectCounts[0];
                case 4:
                    return connectCounts[4];
                default:
                    return null;
                }
            
            } else if (columnIndex == 1) {
                // Return labels.
                switch (rowIndex) {
                case 0:
                    return ULTRAPEERS;
                case 1:
                    return PEERS;
                case 2:
                    return LEAVES;
                case 3:
                    return CONNECTING;
                case 4:
                    return STANDARD;
                default:
                    return null;
                }
            }
            
            return null;
        }

        /**
         * Updates the data model using the specified connection list.
         */
        public void update(EventList<ConnectionItem> connectionList) {
            // The data model is a 5-element array containing the number of 
            // connections with the following states: connecting, ultrapeer,
            // peer, leaf, standard.
            Arrays.fill(connectCounts, 0);

            // Update connection counts.
            for (int i = 0; i < connectionList.size(); i++) {
                ConnectionItem item = connectionList.get(i);
                if (!item.isConnected()) {
                    connectCounts[0]++;
                } else if (item.isUltrapeer()) {
                    connectCounts[1]++;
                } else if (item.isPeer()) {
                    connectCounts[2]++;
                } else if (item.isLeaf()) {
                    connectCounts[3]++;
                } else {
                    connectCounts[4]++;
                }
            }
            
            // Fire event to update table.
            fireTableDataChanged();
        }
    }
}
