package org.limewire.ui.swing.advanced.connection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;

/**
 * Display panel for the connection details table.
 */
public class ConnectionDetailPanel extends JPanel {

    /** Manager instance for connection data. */
    private GnutellaConnectionManager gnutellaConnectionManager;

    private JScrollPane scrollPane = new JScrollPane();
    private ConnectionTable connectionTable = new ConnectionTable();
    private JPopupMenu popupMenu = new JPopupMenu();
    
    /**
     * Constructs the ConnectionDetailPanel to display connections details.
     */
    @Inject
    public ConnectionDetailPanel(GnutellaConnectionManager gnutellaConnectionManager) {
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.setPreferredSize(new Dimension(120, 295));

        // Set table popup handler to display context menu.
        connectionTable.setPopupHandler(new TablePopupHandler() {
            @Override
            public boolean isPopupShowing(int row) {
                // Always return false to prevent color highlighting when popup
                // menu is displayed. See MouseableTable.MenuHighlightPredicate.
                return false;
            }

            @Override
            public void maybeShowPopup(Component component, int x, int y) {
                // Get target row.
                int row = connectionTable.rowAtPoint(new Point(x, y));
                
                // Change selection to include target row.
                if ((row >= 0) && !connectionTable.isRowSelected(row)) {
                    connectionTable.setRowSelectionInterval(row, row);
                }
                
                // Show popup menu.
                popupMenu.show(component, x, y);
            }
        });
        
        // Set table double-click handler to view library.
        connectionTable.setDoubleClickHandler(new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                if (row >= 0) {
                    connectionTable.setRowSelectionInterval(row, row);
                }
                viewLibrary();
            }
        });

        // Add View Library action.
        JMenuItem menuItem = new JMenuItem(I18n.tr("View Files"));
        menuItem.addActionListener(new ViewLibraryAction());
        popupMenu.add(menuItem);
        popupMenu.addSeparator();

        // Add Remove action.
        menuItem = new JMenuItem(I18n.tr("Remove"));
        menuItem.addActionListener(new RemoveConnectionAction());
        popupMenu.add(menuItem);
        
        add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(connectionTable);
    }
    
    /**
     * Initializes the data models in the container.
     */
    public void initData() {
        if (!(connectionTable.getModel() instanceof EventTableModel)) {
            // Create connection list for Swing.  We wrap the actual list in a
            // Swing list to ensure that all events are fired on the UI thread.
            TransformedList<ConnectionItem, ConnectionItem> connectionList = 
                GlazedListsFactory.swingThreadProxyEventList(
                        gnutellaConnectionManager.getConnectionList());

            // Create table format.
            ConnectionTableFormat connectionTableFormat = new ConnectionTableFormat();

            // Set up connection table model.
            connectionTable.setEventList(connectionList, connectionTableFormat);
        }
    }
    
    /**
     * Clears the data models in the container.
     */
    public void clearData() {
        connectionTable.clearEventList();
    }

    /**
     * Triggers a refresh of the data being displayed. 
     */
    public void refresh() {
        connectionTable.refresh();
    }

    /**
     * Displays the libraries for all selected connections.
     */
    private void viewLibrary() {
        // Browse hosts and display all selected connections.
        ConnectionItem[] items = connectionTable.getSelectedConnections();
        for (ConnectionItem item : items) {
            gnutellaConnectionManager.browseHost(item);
        }
    }
    
    /**
     * Action to remove the selected connections. 
     */
    private class RemoveConnectionAction extends AbstractAction {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Remove all selected connections.
            ConnectionItem[] items = connectionTable.getSelectedConnections();
            for (ConnectionItem item : items) {
                gnutellaConnectionManager.removeConnection(item);
            }
            
            connectionTable.clearSelection();
        }
    }
    
    /**
     * Action to display the library for the selected connections.
     */
    private class ViewLibraryAction extends AbstractAction {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            viewLibrary();
        }
    }
}
