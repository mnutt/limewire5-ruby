package org.limewire.nio;

import org.limewire.nio.observer.StubReadWriteObserver;

class StubThrottleListener implements ThrottleListener {
    
    private Object attach;
    private boolean closed;
    private int bandwidthAvailableCalls;
    private Throttle throttle;
    private StubReadWriteObserver rwo;
    private int given;
    
    StubThrottleListener(StubReadWriteObserver rwo, Throttle throttle) { 
        this.attach = rwo;
        this.rwo = rwo;
        this.throttle = throttle;
    }
    
    void setThrottle(Throttle throttle) {
        this.throttle = throttle;
    }
    
    public void setAttachment(Object attachment) { this.attach = attachment; }
    public Object getAttachment() { return attach; }
    public boolean bandwidthAvailable() { bandwidthAvailableCalls++; return !closed; }
    public boolean isOpen() { return !closed; }
    
    void close() { closed = true; }
    void setClosed(boolean closed) { this.closed = closed; }
    int bandwidthAvailableCalls() { return bandwidthAvailableCalls; }
    void clear() { bandwidthAvailableCalls = 0; closed = false; }
    
    public void requestBandwidth() {
        given = throttle.request();
        rwo.setAmountGiven(given);
    }
    
    public void releaseBandwidth() {
        throttle.release(rwo.getAmountLeft());
    }
    
    int given() {
        return given;
    }
}