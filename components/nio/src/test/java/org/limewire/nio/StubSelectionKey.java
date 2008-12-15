/**
 * 
 */
package org.limewire.nio;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

class StubSelectionKey extends SelectionKey {
    
    private volatile int interestOps = 0;
    private int readyOps = 0;
    private volatile boolean valid = true;
    private SelectableChannel channel;
    private Selector selector;
    
    StubSelectionKey(Selector selector, SelectableChannel channel, int ops, Object attachment) {
        this.selector = selector;
        this.channel = channel;
        this.interestOps = ops;
        attach(attachment);
    }
    
    void setReadyOps(int ops) {
        this.readyOps = ops;
    }

    @Override
    public void cancel() {
        valid = false;
    }

    @Override
    public SelectableChannel channel() {
        return channel;
    }

    @Override
    public int interestOps() {
        return interestOps;
    }

    @Override
    public SelectionKey interestOps(int ops) {
        if(!valid)
            throw new CancelledKeyException();
        this.interestOps = ops;
        return this;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public int readyOps() {
        if(!valid)
            throw new CancelledKeyException();
        return readyOps;
    }

    @Override
    public Selector selector() {
        return selector;
    }
}