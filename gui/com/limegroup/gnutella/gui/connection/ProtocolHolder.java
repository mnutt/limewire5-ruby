package com.limegroup.gnutella.gui.connection;


import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.gui.I18n;


/**
 * Wrapper class that acts as a comparable for the dropped i/o info.
 * @author sam berlin
 */
public final class ProtocolHolder implements Comparable<ProtocolHolder> {
	
	/**
	 * Variable for the string representation
	 */
	private String _string;
	
	private static final String LEAF =
        I18n.tr("Leaf");
        
    private static final String ULTRAPEER =
        I18n.tr("Ultrapeer");
        
    private static final String PEER =
        I18n.tr("Peer");
        
    private static final String STANDARD =
        I18n.tr("0.6");    

	/**
	 * Variable for the info.
	 */
	private ConnectionCapabilities _c;

	/**
	 * The constructor sets  the connection
	 */
	public ProtocolHolder(ConnectionCapabilities c) {
	    _c = c;
        if( c.isSupernodeClientConnection() )
            _string = LEAF;
        else if( c.isClientSupernodeConnection() )
            _string = ULTRAPEER;
        else if( c.isSupernodeSupernodeConnection() )
            _string = PEER;
        else
            _string = STANDARD;
	}
	
	/**
	 * Add up the two things and see which is larger.
	 */
	public int compareTo(ProtocolHolder other) {
	    return weightHostInfo(_c) - weightHostInfo(other._c);
	}
	
    private static int weightHostInfo(ConnectionCapabilities c) {
        //Assign weight based on bandwidth:
        //4. ultrapeer->ultrapeer
        //3. old-fashioned (unrouted)
        //2. ultrapeer->leaf
        //1. leaf->ultrapeer
        if (c.isSupernodeConnection()) {
            if (c.isClientSupernodeConnection())
                return 1;
            else
                return 4;                
        } else if (c.isSupernodeClientConnection()) {
            return 2;
        }
        return 3;
    }	

	/**
	 *
	 * @return the formatted string
	 */
	@Override
    public String toString() {
		return _string;
	}
}
