package org.limewire.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class FakeSelectionKey extends SelectionKey {
    private int ops;
    
    public FakeSelectionKey(Object attch) {
        this(attch, true, true);
    }
    
    public FakeSelectionKey(Object attch, boolean r, boolean w) {
        super.attach(attch);
        ops |= SelectionKey.OP_READ;
        ops |= SelectionKey.OP_WRITE;
    }
    
    @Override
    public void cancel() {}
    @Override
    public SelectableChannel channel() { return null; }
    @Override
    public int interestOps() { return ops; }
    @Override
    public SelectionKey interestOps(int ops) { this.ops = ops; return this; }
    @Override
    public boolean isValid() { return true; }
    @Override
    public int readyOps() { return ops; }
    @Override
    public Selector selector() { return null; }
}