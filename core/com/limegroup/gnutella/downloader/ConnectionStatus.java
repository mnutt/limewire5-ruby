package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.tigertree.HashTree;

/**
 * Simple class that enumerates values for the status of
 * requesting a file.
 *
 * Possible options are:
 * <ul>
 * <li>NoFile (the server is not giving us the file)</li>
 * <li>Queued (the server queued us)</li>
 * <li>Connected (we are connected and should download)</li>
 * <li>NoData (we have no data to request)</li>
 * <li>PartialData (the server has other data to use)</li>
 * <li>ThexResponse (the server just gave us a HashTree)</li>
 * </ul>
 */
public class ConnectionStatus {
    
    static final int TYPE_NO_FILE = 0;
    static final int TYPE_QUEUED = 1;
    static final int TYPE_CONNECTED = 2;
    static final int TYPE_NO_DATA = 3;
    static final int TYPE_PARTIAL_DATA = 4;
    static final int TYPE_THEX_RESPONSE = 5;
    
    /**
     * The status of this connection.
     */
    private final int STATUS;
    
    /**
     * The queue position.  Only valid if queued.
     */
    private final int QUEUE_POSITION;
    
    /**
     * The queue poll time.  Only valid if queued.
     */
    private final int QUEUE_POLL_TIME;
    
    /**
     * The hash tree.  Only valid if thex response.
     */
    private final HashTree HASH_TREE;
    
    /**
     * The sole NO_FILE instance.
     */
    private static final ConnectionStatus NO_FILE =
        new ConnectionStatus(TYPE_NO_FILE);
        
    /**
     * The sole CONNECTED instance.
     */
    private static final ConnectionStatus CONNECTED =
        new ConnectionStatus(TYPE_CONNECTED);
        
    /**
     * The sole NO_DATA instance.
     */
    private static final ConnectionStatus NO_DATA =
        new ConnectionStatus(TYPE_NO_DATA);
        
    /**
     * The sole PARTIAL_DATA instance.
     */
    private static final ConnectionStatus PARTIAL_DATA =
        new ConnectionStatus(TYPE_PARTIAL_DATA);
       
    /**
     * Constructs a ConnectionStatus of the specified status.
     */
    private ConnectionStatus(int status) {
        if(status == TYPE_QUEUED || status == TYPE_THEX_RESPONSE)
            throw new IllegalArgumentException();
        STATUS = status;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
        HASH_TREE = null;
    }
    
    /**
     * Constructs a ConnectionStatus for being queued.
     */
    private ConnectionStatus(int status, int queuePos, int queuePoll) {
        if(status != TYPE_QUEUED)
            throw new IllegalArgumentException();
            
        STATUS = status;
        QUEUE_POSITION = queuePos;
        QUEUE_POLL_TIME = queuePoll;
        HASH_TREE = null;
    }
    
    private ConnectionStatus(int status, HashTree tree) {
        if(status != TYPE_THEX_RESPONSE)
            throw new IllegalArgumentException();
        if(tree == null)
            throw new NullPointerException("null tree");
            
        STATUS = status;
        HASH_TREE = tree;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
    }
    
    /**
     * Returns a ConnectionStatus for the server not having the file.
     */
    static ConnectionStatus getNoFile() {
        return NO_FILE;
    }
    
    /**
     * Returns a ConnectionStatus for being connected.
     */
    static ConnectionStatus getConnected() {
        return CONNECTED;
    }
    
    /**
     * Returns a ConnectionStatus for us not having data.
     */
    static ConnectionStatus getNoData() {
        return NO_DATA;
    }
    
    /**
     * Returns a ConnectionStatus for the server having other partial data.
     */
    static ConnectionStatus getPartialData() {
        return PARTIAL_DATA;
    }
    
    /**
     * Returns a ConnectionStatus for being queued with the specified position
     * and poll time (in seconds).
     */
    static ConnectionStatus getQueued(int pos, int poll) {
        // convert to milliseconds & add an extra second.
        poll *= 1000;
        poll += 1000;
        return new ConnectionStatus(TYPE_QUEUED, pos, poll);
    }
    
    /**
     * Returns a ConnectionStatus for having a THEX tree.
     */
    static ConnectionStatus getThexResponse(HashTree tree) {
        return new ConnectionStatus(TYPE_THEX_RESPONSE, tree);
    }
    
    /**
     * Returns the type of this ConnectionStatus.
     */
    int getType() {
        return STATUS;
    }
    
    /**
     * Determines if this is a NoFile ConnectionStatus.
     */
    boolean isNoFile() {
        return STATUS == TYPE_NO_FILE;
    }
    
    /**
     * Determines if this is a Connected ConnectionStatus.
     */    
    public boolean isConnected() {
        return STATUS == TYPE_CONNECTED;
    }
    
    /**
     * Determines if this is a NoData ConnectionStatus.
     */
    boolean isNoData() {
        return STATUS == TYPE_NO_DATA;
    }
    
    /**
     * Determines if this is a PartialData ConnectionStatus.
     */
    boolean isPartialData() {
        return STATUS == TYPE_PARTIAL_DATA;
    }
    
    /**
     * Determines if this is a Queued ConnectionStatus.
     */
    boolean isQueued() {
        return STATUS == TYPE_QUEUED;
    }
    
    /**
     * Determines if this is a ThexResponse ConnectionStatus.
     */
    public boolean isThexResponse() {
        return STATUS == TYPE_THEX_RESPONSE;
    }
    
    /**
     * Determines the queue position.  Throws IllegalStateException if called
     * when the status is not queued.
     */
    int getQueuePosition() {
        if(!isQueued())
            throw new IllegalStateException();
        return QUEUE_POSITION;
    }
    
    /**
     * Determines the queue poll time (in milliseconds).
     * Throws IllegalStateException if called when the status is not queued.
     */
    int getQueuePollTime() {
        if(!isQueued())
            throw new IllegalStateException();
        return QUEUE_POLL_TIME;
    }
    
    /**
     * Returns the HashTree.
     * Throws IllegalStateException if called when the status is not ThexResponse.
     */
    public HashTree getHashTree() {
        if(!isThexResponse())
            throw new IllegalStateException();
        return HASH_TREE;
    }

    @Override
    public String toString() {
        switch(STATUS) {
        case TYPE_NO_FILE: return "No File";
        case TYPE_QUEUED: return "Queued";
        case TYPE_CONNECTED: return "Connected";
        case TYPE_NO_DATA: return "No Data";
        case TYPE_PARTIAL_DATA: return "Partial Data";
        case TYPE_THEX_RESPONSE: return "Thex Response";
        default: return "Unknown: " + STATUS;
        }
    }
}
