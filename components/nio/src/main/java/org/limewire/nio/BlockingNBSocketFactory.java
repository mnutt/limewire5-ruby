package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** 
 * An <code>NBSocketFactory</code> that returns {@link BlockingSocketAdapter}
 * sockets. 
 */
public class BlockingNBSocketFactory extends NBSocketFactory {

    @Override
    public BlockingSocketAdapter createSocket() throws IOException {
        return new BlockingSocketAdapter();
    }

    @Override
    public BlockingSocketAdapter createSocket(String host, int port) throws IOException,
            UnknownHostException {
        return new BlockingSocketAdapter(host, port);
    }

    @Override
    public BlockingSocketAdapter createSocket(InetAddress host, int port) throws IOException {
        return new BlockingSocketAdapter(host, port);
    }

    @Override
    public BlockingSocketAdapter createSocket(String host, int port, InetAddress localHost,
            int localPort) throws IOException, UnknownHostException {
        return new BlockingSocketAdapter(host, port, localHost, localPort);
    }

    @Override
    public BlockingSocketAdapter createSocket(InetAddress address, int port,
            InetAddress localAddress, int localPort) throws IOException {
        return new BlockingSocketAdapter(address, port, localAddress, localPort);
    }

}
