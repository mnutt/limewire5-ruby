package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;


/**
 * A socket that allows asynchronous connections but is backed
 * by a legacy I/O socket.
 * <p>
 * This is intended primarily for ease of use debugging via swapping out
 * an {@link NIOSocket} with a {@link BlockingSocketAdapter}, so NIO code can 
 * be compared to regular I/O code.
 */
public class BlockingSocketAdapter extends NBSocket {
    
    /** Observer for when the socket is shutdown. */
    private volatile Shutdownable shutdownObserver;

    public BlockingSocketAdapter() {
        super();
    }
    
    public BlockingSocketAdapter(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }
    
    public BlockingSocketAdapter(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }
    
    public BlockingSocketAdapter(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }
    
    public BlockingSocketAdapter(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    /**
     * This will spawn a new thread and perform the blocking connect on that thread.
     * The observer will be notified of the changes.
     */
    @Override
    public boolean connect(final SocketAddress addr, final int timeout, 
                           final ConnectObserver observer) {
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                try {
                    connect(addr, timeout);
                    observer.handleConnect(BlockingSocketAdapter.this);
                } catch(IOException x) {
                    observer.shutdown();
                }
            }
        }, "BlockingSocketEmulator");
        return false;
    }
    
    /**
     * Sets an observer for being shutdown.
     */
    @Override
    public void setShutdownObserver(Shutdownable observer) {
        this.shutdownObserver = observer;
    }
    
    /**
     * Closes the socket and notifies the shutdown observer.
     */
    @Override
    public void close() throws IOException {
        shutdownObserver.shutdown();
        super.close();
    }
    
}
