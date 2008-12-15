package org.limewire.swarm;

import java.net.SocketAddress;

import org.limewire.collection.IntervalSet;

/**
 * Represents a source to download from.
 * 
 * Contains address and path information that is used by a {@link SwarmSourceDownloader}
 * to download {@link #getAvailableRanges() available ranges} from the source.
 * 
 * {@link SwarmSourceListener} can register to be notified of any events concerning
 * this source.
 * 
 * {@link SwarmSourceDownloader} are supposed to fire those events on the source
 * by calling the respective methods.
 */
public interface SwarmSource {

    /**
     * Returns the socket address for the underlying download source.
     */
    SocketAddress getAddress();

    /**
     * Returns the path on the server to the download source.
     */
    String getPath();

    /**
     * Returns the available ranges the source offers.
     */
    IntervalSet getAvailableRanges();

    /**
     * Returns the swarmSourceType for this swarmSource.
     */
    SwarmSourceType getType();

    /**
     * Returns true if we are finished with this swarm source, i.e. if
     * all its available ranges have been downloaded.
     */
    public boolean isFinished();

    /**
     * Adds listener to this swarmsource.
     */
    void addListener(SwarmSourceListener listener);

    /**
     * Removes a listener from this swarmSource.
     */
    void removeListener(SwarmSourceListener listener);

    /**
     * Fires connectFailed event to registerd listeners.
     */
    void connectFailed(SwarmSourceDownloader swarmSourceHandler);

    /**
     * Fires connected event to registerd listeners. Supposed to be called
     * by the {@link SwarmSourceDownloader} when it is connected to the source.
     */
    void connected(SwarmSourceDownloader swarmSourceHandler);

    /**
     * Fires connectionClosed event to registerd Listeners.
     */
    void connectionClosed(SwarmSourceDownloader swarmSourceHandler);

    /**
     * Fires responseProcessed event to registerd Listeners.
     */
    void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmStatus status);

    /**
     * Fires finished event to registerd Listeners.
     */
    void finished(SwarmSourceDownloader swarmSourceHandler);
    
    
    /**
     * The SwarmSource implementation should override the default equals implementation.
     */
    public boolean equals(Object obj);
    
    /**
     * The SwarmSource implementation should override the default hashcode implementation.
     */
    public int hashCode();

}
