package org.limewire.nio.observer;

import java.io.IOException;

public class StubReadWriteObserver implements IOErrorObserver, ReadWriteObserver {
    
    private volatile int amtToUse = Integer.MAX_VALUE;
    private volatile int read = 0;
    private volatile int wrote = 0;
    private volatile boolean shutdown = false;
    private volatile IOException iox = null;
    private volatile int amountGiven = 0;
    private volatile int amountLeft = 0;
    
    public void setAmountGiven(int given) {
        amountGiven = given;
    }
    
    public int getAmountLeft() {
        return amountLeft;
    }
    
    public void handleRead() throws IOException {
        read++;
        amountLeft = Math.max(0, amountGiven - amtToUse);
    }
    
    public boolean handleWrite() throws IOException {
        wrote++;
        amountLeft = Math.max(0, amountGiven - amtToUse);
        return false;
    }
    
    public void handleIOException(IOException iox) {
        this.iox = iox;
    }
    
    public void shutdown() {
        shutdown = true;
    }
    
    public void setAmountToUse(int toUse) { amtToUse = toUse; }
    public int read() { return read; }
    public int wrote() { return wrote; }
    public IOException iox() { return iox; }
    public boolean closed() { return shutdown; }
    
}