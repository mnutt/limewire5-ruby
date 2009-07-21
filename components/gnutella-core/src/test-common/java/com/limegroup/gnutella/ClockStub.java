package com.limegroup.gnutella;

import org.limewire.util.Clock;

import com.google.inject.Singleton;

@Singleton
public class ClockStub implements Clock {
    
    private long nanoTime;
    private long now;
    
    public void setNanoTime(long nanoTime) {
        this.nanoTime = nanoTime;
    }
    
    public void setNow(long now) {
        this.now = now;
    }
    
    public void addNow(long addTime) {
        this.now += addTime;
    }
    
    public void substractNow(long subtractTime) {
        this.now -= subtractTime;
    }
    
    public void addNanoTime(long addTime) {
        this.nanoTime += addTime;
    }
    
    public void subtractNanoTime(long subtractTime) {
        this.nanoTime -= subtractTime;
    }
    
    public long nanoTime() {
        return nanoTime;
    }

    public long now() {
        return now;
    }
}
