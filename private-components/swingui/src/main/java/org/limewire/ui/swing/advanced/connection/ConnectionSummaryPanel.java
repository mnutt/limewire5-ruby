package org.limewire.ui.swing.advanced.connection;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.GnutellaConnectionManager;
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
    private static final String CONNECTED_TO = I18n.tr("Connected to:");
    
    private static final String CONNECTING = I18n.tr("Connecting");
    private static final String LEAVES = I18n.tr("Leaves");
    private static final String PEERS = I18n.tr("Peers");
    private static final String STANDARD = I18n.tr("Standard");
    private static final String ULTRAPEERS = I18n.tr("Ultrapeers");
    
    /** Manager instance for connection data. */
    private GnutellaConnectionManager gnutellaConnectionManager;

    /** List of connections. */
    private TransformedList<ConnectionItem, ConnectionItem> connectionList;

    private JLabel nodeLabel = new JLabel();
    private JLabel summaryLabel = new JLabel();
    private JTable summaryTable = new JTable();
    private SummaryTableModel summaryTableModel = new SummaryTableModel();

    /**
     * Constructs the ConnectionDetailPanel to display connections details.
     */
    @Inject
    public ConnectionSummaryPanel(GnutellaConnectionManager gnutellaConnectionManager) {
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        
        setBorder(BorderFactory.createTitledBorder(""));
        setLayout(new MigLayout("insets 0 0 0 0,fill",
            "[]",
            "[top]12[bottom][top,fill]"));
        setPreferredSize(new Dimension(120, 120));
        setOpaque(false);

        summaryLabel.setText(CONNECTED_TO);

        summaryTable.setModel(summaryTableModel);
        summaryTable.setPreferredSize(new Dimension(120, 120));
        summaryTable.setShowGrid(false);

        // Set column widths.
        summaryTable.getColumnModel().getColumn(0).setPreferredWidth(24);
        summaryTable.getColumnModel().getColumn(1).setPreferredWidth(96);

        // Install renderer to align summary value.
        summaryTable.getColumnModel().getColumn(0).setCellRenderer(new SummaryCellRenderer());

        add(nodeLabel   , "cell 0 0");
        add(summaryLabel, "cell 0 1");
        add(summaryTable, "cell 0 2");
    }

    @Override
    public void setBackground(Color bgColor) {
        super.setBackground(bgColor);
        if (summaryTable != null) {
            summaryTable.setBackground(bgColor);
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
        }
    }
    
    /**
     * Clears the data models in the container.
     */
    public void clearData() {
        connectionList.dispose();
        connectionList = null;
    }

    /**
     * Triggers a refresh of the data being displayed. 
     */
    public void refresh() {
        summaryTableModel.update(connectionList);
    }

    /**
     * Table cell renderer for connection count values in the summary table.
     * Values are right-aligned with a right margin.
     */
    private class SummaryCellRenderer extends DefaultTableCellRenderer {

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
