package com.limegroup.gnutella.chat;

/**
 * The class that serves as the interface between a chat instance and the gui.
 */
public interface InstantMessenger {

    /**
     * Closes the connection to the peer. Subsequent invocations of
     * {@link #send(String)} will return false.
     */
    public void stop();

    /**
     * Sends <code>message</code> to the peer.
     * 
     * @return true, if message was sent successfully; false, if connection was
     *         already closed or the buffer for outgoing messages is full
     */
    public boolean send(String message);

    /**
     * Returns the host of the peer.
     */
    public String getHost();

    /**
     * Returns the port of the peer.
     */
    public int getPort();

    public void start();

    public boolean isOutgoing();

    public boolean isConnected();
    
}
