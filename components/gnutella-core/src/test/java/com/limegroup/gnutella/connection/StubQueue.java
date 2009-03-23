package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;

/**
 * Queue that can handle dropping msgs too.
 */
public class StubQueue extends BasicQueue {
    
    int numToDrop;
    int startDropIn;
    int dropped;
    
    public void setNumToDrop(int i) {
        numToDrop = i;
    }
    
    public void setStartDropIn(int i) {
        startDropIn = i;
    }
    
    @Override
    public void add(Message m) {
        if(size() > 0 && startDropIn-- <= 0 && numToDrop > 0) {
            numToDrop--;
            dropped++;
            super.removeNext();
        }
        
        super.add(m);
    }
    
    @Override
    public Message removeNext() {
        while(size() > 0) {
            if(startDropIn-- <= 0 && numToDrop > 0) {
                numToDrop--;
                dropped++;
                super.removeNext();
                continue;
            } else
                break;
        }
        
        return super.removeNext();
    }
    
    @Override
    public int resetDropped() { 
        int d = dropped;
        dropped = 0;
        return d;
    }
        
    
    /** Returns the number of queued messages. */
    @Override
    public int size() {
        return super.size();
    }
    
    @Override
    public void resetCycle() {}
    
    /** Determines if this is empty. */
    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }
    
}