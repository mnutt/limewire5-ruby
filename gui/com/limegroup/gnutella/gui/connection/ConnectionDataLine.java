package com.limegroup.gnutella.gui.connection;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.TimeRemainingHolder;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;


public final class ConnectionDataLine extends AbstractDataLine<RoutedConnection> {
    
    static final int HOST_IDX = 0;
    static final int STATUS_IDX = 1;
    static final int MESSAGES_IDX = 2;
    static final int MESSAGES_IN_IDX = 3;
    static final int MESSAGES_OUT_IDX = 4;
    static final int BANDWIDTH_IDX = 5;
    static final int BANDWIDTH_IN_IDX = 6;
    static final int BANDWIDTH_OUT_IDX = 7;
    static final int DROPPED_IDX = 8;
    static final int DROPPED_IN_IDX = 9;
    static final int DROPPED_OUT_IDX = 10;
    static final int PROTOCOL_IDX = 11;
    static final int VENDOR_IDX = 12;
    static final int TIME_IDX = 13;
    static final int COMPRESSION_IDX = 14;
    static final int COMPRESSION_IN_IDX = 15;
    static final int COMPRESSION_OUT_IDX = 16;
    static final int SSL_IDX = 17;
    static final int SSL_IN_IDX = 18;
    static final int SSL_OUT_IDX = 19;
    static final int QRP_FULL_IDX = 20;
    static final int QRP_USED_IDX = 21;
    
    /**
     * Add the columns to static array _in the proper order_.
     * The *_IDX variables above need to match the corresponding
     * column's position in this array.
     */
    private static final LimeTableColumn[] ltColumns =
    {
        new LimeTableColumn(HOST_IDX, "CV_COLUMN_HOST", I18n.tr("Host"),
                218, true, String.class),
        
        new LimeTableColumn(STATUS_IDX, "CV_COLUMN_STATUS", I18n.tr("Status"),
                70, true, String.class),
                
        new LimeTableColumn(MESSAGES_IDX, "CV_COLUMN_MESSAGE", I18n.tr("Messages (I/O)"),
                97, true, MessagesHolder.class),

        new LimeTableColumn(MESSAGES_IN_IDX, "CV_COLUMN_MESSAGE_IN", I18n.tr("Messages In"),
                97, false, Integer.class),

        new LimeTableColumn(MESSAGES_OUT_IDX, "CV_COLUMN_MESSAGE_OUT", I18n.tr("Messages Out"),
                97, false, Integer.class),

        new LimeTableColumn(BANDWIDTH_IDX, "CV_COLUMN_BANDWIDTH", I18n.tr("Bandwidth (I/O)"),
                115, true, BandwidthHolder.class),

        new LimeTableColumn(BANDWIDTH_IN_IDX, "CV_COLUMN_BANDWIDTH_IN", I18n.tr("Bandwidth In"),
                115, false, BandwidthHolder.class),

        new LimeTableColumn(BANDWIDTH_OUT_IDX, "CV_COLUMN_BANDWIDTH_OUT", I18n.tr("Bandwidth Out"),
                115, false, BandwidthHolder.class),

        new LimeTableColumn(DROPPED_IDX, "CV_COLUMN_DROPPED", I18n.tr("Dropped (I/O)"),
                92, true, DroppedHolder.class),

        new LimeTableColumn(DROPPED_IN_IDX, "CV_COLUMN_DROPPED_IN", I18n.tr("Dropped In"),
                92, false, DroppedHolder.class),

        new LimeTableColumn(DROPPED_OUT_IDX, "CV_COLUMN_DROPPED_OUT", I18n.tr("Dropped Out"),
                92, false, DroppedHolder.class),

        new LimeTableColumn(PROTOCOL_IDX, "CV_COLUMN_PROTOCOL", I18n.tr("Protocol"),
                60, true, ProtocolHolder.class),
                
        new LimeTableColumn(VENDOR_IDX, "CV_COLUMN_VENDOR", I18n.tr("Vendor/Version"),
                116, true, String.class),
                
        new LimeTableColumn(TIME_IDX, "CV_COLUMN_TIME", I18n.tr("Time"),
                44, true, TimeRemainingHolder.class),
                
        new LimeTableColumn(COMPRESSION_IDX, "CV_COLUMN_COMPRESSION", I18n.tr("Compressed (I/O)"),
                114, false, DroppedHolder.class),
                        
        new LimeTableColumn(COMPRESSION_IN_IDX, "CV_COLUMN_COMPRESSION_IN", I18n.tr("Compressed In"),
                114, false, DroppedHolder.class),
                                
        new LimeTableColumn(COMPRESSION_OUT_IDX, "CV_COLUMN_COMPRESSION_OUT", I18n.tr("Compressed Out"),
                114, false, DroppedHolder.class),
                
        new LimeTableColumn(SSL_IDX, "CV_COLUMN_SSL", I18n.tr("SSL Overhead (I/O)"), 
                100, false, DroppedHolder.class),
                        
        new LimeTableColumn(SSL_IN_IDX, "CV_COLUMN_SSL_IN", I18n.tr("SSL Overhead In"), 
                100, false, DroppedHolder.class),
                                
        new LimeTableColumn(SSL_OUT_IDX, "CV_COLUMN_SSL_OUT", I18n.tr("SSL Overhead Out"), 
                100, false, DroppedHolder.class),
                
        new LimeTableColumn(QRP_FULL_IDX, "CV_COLUMN_QRP_FULL", I18n.tr("QRP (%)"),
                70, false, QRPHolder.class),
                
        new LimeTableColumn(QRP_USED_IDX, "CV_COLUMN_QRP_USED", I18n.tr("QRP Empty"),
                70, false, String.class)
    };
    
    /**
     * String for connecting status
     */
    private static final String CONNECTING_STRING =
        I18n.tr("Connecting...");

    /**
     * String for outgoing status
     */
    private static final String OUTGOING_STRING =
        I18n.tr("Outgoing");

    /**
     * String for incoming status
     */
    private static final String INCOMING_STRING =
        I18n.tr("Incoming");

    /**
     * String for 'Connected on' tooltip
     */
    private static final String CONNECTED_ON =
        I18n.tr("Connected on");

    /**
     * Cached host
     */
    private volatile String _host;

    /**
     * Cached status
     */
    private String _status;

    /**
     * Time this connected or initialized
     */
    private long _time;

    /**
     * Whether or not this dataline is in the 'connecting' state
     */
    private boolean _isConnecting = true;

    /**
     * Variable for whether or not the host name has been resolved for
     * this connection.
     */
    private boolean _hasResolvedAddress = false;

    /**
     * Boolean for whether or not the 'host' of a line has changed.
     */
    private static volatile boolean _hostChanged = false;

    /**
     * Boolean for whether a line has updated from connecting to connected
     */
    private static boolean _updated = false;

    /**
     * Number of columns
     */
    public int getColumnCount() { return ltColumns.length; }

    /**
     * Sets up the dataline for use with the connection
     */
    @Override
    public void initialize(RoutedConnection conn) {
        super.initialize(conn);

        _host = initializer.getAddress();

        _status = CONNECTING_STRING;
        _time = System.currentTimeMillis();
    }

    /**
     * Returns the value for the specified index.
     */
    public Object getValueAt(int idx) {
        switch(idx) {
            case HOST_IDX:
                if(!_hasResolvedAddress // hasn't yet resolved address
                   && !_isConnecting // must be connected
                   && (System.currentTimeMillis() - _time) > 10000)
					assignHostName();
                return _host;
            case STATUS_IDX: return _status;
            case MESSAGES_IDX:
                if (_isConnecting) return null;
                return new MessagesHolder(
                    initializer.getConnectionMessageStatistics().getNumMessagesReceived(),
                    initializer.getConnectionMessageStatistics().getNumMessagesSent()
                );
            case MESSAGES_IN_IDX:
                if (_isConnecting) return null;
                return initializer.getConnectionMessageStatistics().getNumMessagesReceived();
            case MESSAGES_OUT_IDX:
                if (_isConnecting) return null;
                return initializer.getConnectionMessageStatistics().getNumMessagesSent();
            case BANDWIDTH_IDX:
                if (_isConnecting) return null;
                return new BandwidthHolder(
                    initializer.getMeasuredDownstreamBandwidth(),
                    initializer.getMeasuredUpstreamBandwidth()
                );
            case BANDWIDTH_IN_IDX:
                if (_isConnecting) return null;
                return new BandwidthHolder( initializer.getMeasuredDownstreamBandwidth() );
            case BANDWIDTH_OUT_IDX:
                if (_isConnecting) return null;
                return new BandwidthHolder( initializer.getMeasuredUpstreamBandwidth() );
            case DROPPED_IDX:
                if (_isConnecting) return null;
                // NOTE: this use to be getPercent[Sent|Received]Dropped
                // However that had the side-effect of altering the
                // connection's stats.
                // This provides more accurate statistics anyway,
                // rather than a snapshot-erase-style number.
                return new DroppedHolder(
                     initializer.getConnectionMessageStatistics().getNumReceivedMessagesDropped() /
                       ( initializer.getConnectionMessageStatistics().getNumMessagesReceived() + 1.0f ),
                    initializer.getConnectionMessageStatistics().getNumSentMessagesDropped() /
                       ( initializer.getConnectionMessageStatistics().getNumMessagesSent() + 1.0f )
                );
            case DROPPED_IN_IDX:
                if (_isConnecting) return null;
                return new DroppedHolder(
                     initializer.getConnectionMessageStatistics().getNumReceivedMessagesDropped() /
                       ( initializer.getConnectionMessageStatistics().getNumMessagesReceived() + 1.0f )
                );
            case DROPPED_OUT_IDX:
                if (_isConnecting) return null;
                return new DroppedHolder(
                    initializer.getConnectionMessageStatistics().getNumSentMessagesDropped() /
                       ( initializer.getConnectionMessageStatistics().getNumMessagesSent() + 1.0f )
                );
            case PROTOCOL_IDX:  return new ProtocolHolder( initializer.getConnectionCapabilities() );
            case VENDOR_IDX:
                if (_isConnecting) return null;
                String vendor = initializer.getConnectionCapabilities().getUserAgent();
                return vendor == null ? "" : vendor;
            case TIME_IDX:
                return new TimeRemainingHolder( (int)(
                    (System.currentTimeMillis() - _time) / 1000) );
            case COMPRESSION_IDX:
                if (_isConnecting) return null;
                return new DroppedHolder(
                    initializer.getConnectionBandwidthStatistics().getReadSavedFromCompression(),
                    initializer.getConnectionBandwidthStatistics().getSentSavedFromCompression() );
            case COMPRESSION_IN_IDX:
                if (_isConnecting) return null;
                return new DroppedHolder(
                    initializer.getConnectionBandwidthStatistics().getReadSavedFromCompression() );
            case COMPRESSION_OUT_IDX:
                if (_isConnecting) return null;
                return new DroppedHolder(
                    initializer.getConnectionBandwidthStatistics().getSentSavedFromCompression() );
            case SSL_IDX:
                return new DroppedHolder(
                        initializer.getConnectionBandwidthStatistics().getReadLostFromSSL(),
                        initializer.getConnectionBandwidthStatistics().getSentLostFromSSL() );
            case SSL_IN_IDX:
                return new DroppedHolder(
                        initializer.getConnectionBandwidthStatistics().getReadLostFromSSL() );
            case SSL_OUT_IDX:
                return new DroppedHolder(
                        initializer.getConnectionBandwidthStatistics().getSentLostFromSSL() );
            case QRP_FULL_IDX:
                if(_isConnecting) return null;
                return new QRPHolder(
                    initializer.getRoutedConnectionStatistics().getQueryRouteTablePercentFull(),
                    initializer.getRoutedConnectionStatistics().getQueryRouteTableSize());
            case QRP_USED_IDX:
                if(_isConnecting) return null;  
                int empty = initializer.getRoutedConnectionStatistics().getQueryRouteTableEmptyUnits();
                int inuse = initializer.getRoutedConnectionStatistics().getQueryRouteTableUnitsInUse();
                if(empty == -1 || inuse == -1)
                    return null;
                else
                    return empty + " / " + inuse;
        }
        return null;
    }

	/**
	 * Helper method that launches a separate thread to look up the host name
	 * of the given connection.  The thread is necessary because the lookup
	 * can take considerable time.
	 */
	private void assignHostName() {
		// put this outside of the runnable so multiple attempts aren't done.
        _hasResolvedAddress = true;

	    BackgroundExecutorService.schedule(new HostAssigner(this));
	}

	/**
	 * Return the table column for this index.
	 */
	public LimeTableColumn getColumn(int idx) {
	    return ltColumns[idx];
    }
    
    public boolean isClippable(int idx) {
        return true;
    }
    
    public int getTypeAheadColumn() {
        return HOST_IDX;
    }

	public boolean isDynamic(int idx) {
	    switch(idx) {
	        case MESSAGES_IDX:
	        case MESSAGES_IN_IDX:
	        case MESSAGES_OUT_IDX:
	        case BANDWIDTH_IDX:
	        case BANDWIDTH_IN_IDX:
	        case BANDWIDTH_OUT_IDX:
	        case DROPPED_IDX:
	        case DROPPED_IN_IDX:
	        case DROPPED_OUT_IDX:
	        case COMPRESSION_IDX:
	        case COMPRESSION_IN_IDX:
	        case COMPRESSION_OUT_IDX:
            case SSL_IDX:
            case SSL_IN_IDX:
            case SSL_OUT_IDX:
            case QRP_FULL_IDX:
            case QRP_USED_IDX:
	            return true;
	        case HOST_IDX:
	            // if a host changed, set it to false for the future
	            // and return true.  otherwise return false.
	            if ( _hostChanged ) {
	                _hostChanged = false;
	                return true;
	            } else {
	                return false;
	            }
	        case VENDOR_IDX:
	        case STATUS_IDX:
	        case PROTOCOL_IDX:
	            if ( _updated ) {
	                _updated = false;
	                return true;
	            } else {
	                return false;
	            }

	    }
	    return false;
	}
	
	boolean isPeer() {
	    return initializer.getConnectionCapabilities().isSupernodeSupernodeConnection();
    }
    
    boolean isUltrapeer() {
        return initializer.getConnectionCapabilities().isClientSupernodeConnection();
    }
    
    boolean isLeaf() {
        return initializer.isSupernodeClientConnection();
    }
    
    boolean isConnecting() {
        return _isConnecting;
    }

    /**
     * Updates this connection from a 'connecting' to a 'connected' state.
     */
    @Override
    public void update() {
        _isConnecting = false;

        boolean isOutgoing = initializer.isOutgoing();

        _status = isOutgoing ? OUTGOING_STRING : INCOMING_STRING;

        _host = initializer.getInetAddress().getHostAddress();

        // once it's connected, add it to the dictionary for host entry
        if ( isOutgoing )
            ConnectionMediator.instance().addKnownHost(
                _host, initializer.getPort()
            );

        _updated = true;
        _time = initializer.getConnectionTime();
    }
    
    /**
     * Returns whether or not this line is connected.
     */
    public boolean isConnected() {
        return !_isConnecting;
    }

    /**
     * Returns the ToolTip text for this DataLine.
     * Display some of the finer connection information.
     */
    @Override
    public String[] getToolTipArray(int col) {
        Properties p = initializer.getConnectionCapabilities().getHeadersRead().props();
        
        List<String> tips = new ArrayList<String>();
        
        if ( p == null ) {
            // for the lazy .4 connections (yes, some are still there)
            tips.add(CONNECTED_ON + " " + GUIUtils.msec2DateTime(_time));
        } else {
            tips.add(CONNECTED_ON + " " + GUIUtils.msec2DateTime(_time));
            tips.add("");
            
            String k;
            Enumeration ps = p.propertyNames();
            while(ps.hasMoreElements()) {
                k = (String)ps.nextElement();
                tips.add(k + ": " + p.getProperty(k));
            }
        }
        return tips.toArray(new String[0]);
    }
    
    /**
     * Assigns the host field to the line without holding an explicit
     * reference to it.
     */
    private static class HostAssigner implements Runnable {
        private final WeakReference<ConnectionDataLine> line;
        
        HostAssigner(ConnectionDataLine cdl) {
            line = new WeakReference<ConnectionDataLine>(cdl);
        }
        
        public void run() {
            ConnectionDataLine cdl = line.get();
            if(cdl != null) {
                try {
    				cdl._host = InetAddress.getByName(cdl._host).getHostName();
    			    ConnectionDataLine._hostChanged = true;
    		    } catch (UnknownHostException ignored) {}
            }
        }
    }
}
