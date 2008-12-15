package com.limegroup.gnutella.gui.connection;

import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.gui.tables.BasicDataLineModel;

public final class ConnectionModel extends BasicDataLineModel<ConnectionDataLine, RoutedConnection> {
        
    ConnectionModel() {
        super( ConnectionDataLine.class );
    }
    
    /**
     * Override default update since we don't cache anything.
     * Specifically, we do NOT want to call update on the DataLine,
     * since that will make it think it's no longer connecting.
     * @return null
     */
    @Override
    public Object refresh() {
        fireTableRowsUpdated(0, getRowCount());
        return null;
    }
    
    /**
     * Determines the amount of lines that are in the connecting state.
     */
    public int getConnectingCount() {
        int total = 0;
        for(int i = 0; i < getRowCount(); i++) {
            if(!get(i).isConnected())
                total++;
        }
        return total;
    }
    
    /**
     * Gets the connection info.
     */
    public int[] getConnectionInfo() {
        int[] ret = new int[5];
        for(int i = 0; i < getRowCount(); i++) {
            ConnectionDataLine line = get(i);
            if(line.isConnecting())
                ret[0]++;
            else if(line.isUltrapeer())
                ret[1]++;
            else if(line.isPeer())
                ret[2]++;
            else if(line.isLeaf())
                ret[3]++;
            else
                ret[4]++;
        }
        return ret;
    }            
    
    /**
     * Creates a new ConnectionDataLine
     */
    @Override
    public ConnectionDataLine createDataLine() {
        return new ConnectionDataLine();
    }
    
}