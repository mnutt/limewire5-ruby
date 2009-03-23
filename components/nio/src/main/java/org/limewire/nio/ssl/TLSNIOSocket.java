package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.NIOSocket;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.ConnectObserver;

/**
 * A <code>NIOSocket</code> that uses TLS for transfer encoding.
 * <p>
 * <code>TLSNIOSocket</code> is currently hardcoded to only support the cipher suite
 * <code>TLS_DH_anon_WITH_AES_128_CBC_SHA</code>.
 */
public class TLSNIOSocket extends NIOSocket {

    private final static Log LOG = LogFactory.getLog(TLSNIOSocket.class);
    
    private volatile SSLReadWriteChannel tlsLayer;
    private volatile InterestReadableByteChannel baseReader;
    private volatile InterestWritableByteChannel baseWriter;

    public TLSNIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public TLSNIOSocket(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }

    public TLSNIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public TLSNIOSocket(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }

    public TLSNIOSocket() throws IOException {
        super();
    }
    
    TLSNIOSocket(Socket socket) {
        super(socket);
    }
    
    @Override
    public boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) {
        return super.connect(addr, timeout, new TLSConnectInitializer(addr, observer));
    }
    
    @Override
    protected InterestReadableByteChannel getBaseReadChannel() {
        if(baseReader == null) {
            tlsLayer.setReadChannel(super.getBaseReadChannel());
            baseReader = tlsLayer;
        }
        return baseReader;
    }

    @Override
    protected InterestWritableByteChannel getBaseWriteChannel() {
        if(baseWriter == null) {
            tlsLayer.setWriteChannel(super.getBaseWriteChannel());
            baseWriter = tlsLayer;
        }
        return baseWriter;
    }
    
    @Override
    protected void initIncomingSocket() {
        super.initIncomingSocket();
        tlsLayer = new SSLReadWriteChannel(SSLUtils.getTLSContext(), SSLUtils.getExecutor(), NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
        tlsLayer.initialize(getRemoteSocketAddress(), SSLUtils.getTLSCipherSuites(), false, false);
    }

    @Override
    protected void initOutgoingSocket() throws IOException {
        super.initOutgoingSocket();
        tlsLayer = new SSLReadWriteChannel(SSLUtils.getTLSContext(), SSLUtils.getExecutor(), NIODispatcher.instance().getBufferCache(), NIODispatcher.instance().getScheduledExecutorService());
    }
    
    @Override
    protected void shutdownObservers() {
        if(tlsLayer != null)
            tlsLayer.shutdown();
        super.shutdownObservers();
    }
    
    /* package */ SSLReadWriteChannel getSSLChannel() {
        return tlsLayer;
    }
    
    @Override
    /* Overridden to retrieve the soTimeout from the socket if we're still handshaking. */
    public long getReadTimeout() {
        if(tlsLayer != null && tlsLayer.isHandshaking()) {
            try {
                return getSoTimeout();
            } catch(SocketException se) {
                return 0;
            }
        } else {
            return super.getReadTimeout();
        }
    }
    
    /**
     * A delegating connector that forces the TLS Layer to be initialized
     * prior to informing the real <code>ConnectObserver</code> about the connection.
     */
    private class TLSConnectInitializer implements ConnectObserver {
        private final ConnectObserver delegate;
        private final SocketAddress addr;
        
        public TLSConnectInitializer(SocketAddress addr, ConnectObserver delegate) {
            this.delegate = delegate;
            this.addr = addr;
        }

        public void handleConnect(Socket socket) throws IOException {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Initializing TLS connection to " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + tlsLayer.isOpen() +
                        ", handshaking " + tlsLayer.isHandshaking());
            }
            tlsLayer.initialize(addr, SSLUtils.getTLSCipherSuites(), true, false);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Initialized TLS connection to " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + tlsLayer.isOpen() +
                        ", handshaking " + tlsLayer.isHandshaking());
            }
            delegate.handleConnect(socket);
        }

        public void handleIOException(IOException iox) {
            if(LOG.isDebugEnabled()) {
                LOG.debug(iox + ", " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + tlsLayer.isOpen() +
                        ", handshaking " + tlsLayer.isHandshaking());
            }
            delegate.handleIOException(iox);
        }

        public void shutdown() {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Shutting down TLS connection to " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + tlsLayer.isOpen() +
                        ", handshaking " + tlsLayer.isHandshaking());
            }
            delegate.shutdown();
        }        
    }
}
