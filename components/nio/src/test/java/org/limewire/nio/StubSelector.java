/**
 * 
 */
package org.limewire.nio;

import java.io.IOException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


@SuppressWarnings("unchecked")
class StubSelector extends AbstractSelector {
    
    private Set keys = new HashSet();
    private Set selectedKeys = new HashSet();
    
    StubSelector() {
        super(null);
    }

    @Override
    protected void implCloseSelector() throws IOException {
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        if(!(ch instanceof StubChannel))
            throw new IllegalSelectorException();
        
        StubSelectionKey key = new StubSelectionKey(this, ch, ops, att);
        keys.add(key);
        return key;
    }

    @Override
    public Set keys() {
        return keys;
    }

    @Override
    public int select() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int select(long timeout) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set selectedKeys() {
        return selectedKeys;
    }

    @Override
    public int selectNow() throws IOException {
        selectedKeys.clear();
        for(Iterator i = keys.iterator(); i.hasNext();) {
            StubSelectionKey key = (StubSelectionKey)i.next();
            StubChannel channel = (StubChannel)key.channel();
            int ready = channel.readyOps() & key.interestOps();
            if(ready != 0) {
                key.setReadyOps(ready);
                selectedKeys.add(key);
            }
        }
        return selectedKeys.size();
    }

    @Override
    public Selector wakeup() {
        return this;
    }
    
}