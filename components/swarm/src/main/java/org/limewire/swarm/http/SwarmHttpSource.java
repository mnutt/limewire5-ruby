package org.limewire.swarm.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmSourceType;
import org.limewire.swarm.impl.AbstractSwarmSource;
import org.limewire.swarm.impl.LoggingSwarmSourceListener;
import org.limewire.swarm.impl.ReconnectingSwarmSourceListener;
import org.limewire.util.URIUtils;

/**
 * Representation of a swarm source that can be downloaded from over http.
 */
public class SwarmHttpSource extends AbstractSwarmSource {

    private final SocketAddress socketAddress;

    private final String path;

    private IntervalSet availableRanges = new IntervalSet();

    private final String id;

    public SwarmHttpSource(URI uri, Range range) {
        int port = URIUtils.getPort(uri) == -1 ? 80 : URIUtils.getPort(uri); //default to port 80 if no port can be found
        String host = uri.getHost();
        this.socketAddress = new InetSocketAddress(host, port);
        this.path = uri.getPath();
        this.availableRanges.add(range);
        addListener(new ReconnectingSwarmSourceListener());
        addListener(new LoggingSwarmSourceListener());
        id = uri.toString() + "-" + range.toString();
    }

    public SwarmHttpSource(URI uri, long fileSize) {
        this(uri, Range.createRange(0, fileSize - 1));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#getAddress()
     */
    public SocketAddress getAddress() {
        return socketAddress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#getPath()
     */
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Source for: " + socketAddress + ", uri: " + path + ", id: "
                + System.identityHashCode(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#getAvailableRanges()
     */
    public IntervalSet getAvailableRanges() {
        return availableRanges;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSource#getType()
     */
    public SwarmSourceType getType() {
        return SwarmSourceType.HTTP;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SwarmHttpSource)) {
            return false;
        }

        SwarmHttpSource source = (SwarmHttpSource) obj;
        if (getType() != source.getType()) {
            return false;
        }

        return id.equals(source.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static boolean isValidSource(URI uri) {
        return uri.getHost() != null && ("http".equals(uri.getScheme()) || uri.getScheme() == null);
    }
}
