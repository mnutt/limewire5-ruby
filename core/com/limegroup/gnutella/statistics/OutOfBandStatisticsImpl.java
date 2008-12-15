package com.limegroup.gnutella.statistics;

import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.inspection.InspectablePrimitive;

import com.google.inject.Singleton;

/** A default implementation of {@link OutOfBandStatistics} */
@Singleton
class OutOfBandStatisticsImpl implements OutOfBandStatistics {
    
    private static final int START_MIN_SAMPLE_SIZE = 500;
    private static final int MIN_SUCCESS_RATE = 60;
    private static final int PROXY_SUCCESS_RATE = 80;
    private static final int TERRIBLE_SUCCESS_RATE = 40;
    
    @InspectablePrimitive("oob sample size")
    private AtomicInteger sampleSize = new AtomicInteger(START_MIN_SAMPLE_SIZE);
    @InspectablePrimitive("oob requested")
    private AtomicInteger requested = new AtomicInteger(0);
    @InspectablePrimitive("oob received")
    private AtomicInteger received = new AtomicInteger(0);
    @InspectablePrimitive("oob bypassed")
    private AtomicInteger bypassed = new AtomicInteger(0);
    @InspectablePrimitive("oob sent")
    private AtomicInteger sent = new AtomicInteger(0);
    
    public void addBypassedResponse(int numBypassed) {
        bypassed.addAndGet(numBypassed);
    }
    
    public void addReceivedResponse(int numReceived) {
        received.addAndGet(numReceived);
    }
    
    public void addRequestedResponse(int numRequested) {
        requested.addAndGet(numRequested);
    }
    
    public void addSentQuery() {
        sent.incrementAndGet();
    }
    
    public int getRequestedResponses() {
        return requested.get();
    }
    
    public int getSampleSize() {
        return sampleSize.get();
    }
    
    public void increaseSampleSize() {
        sampleSize.addAndGet(500);
    }

    public double getSuccessRate() {
        double numRequested = requested.doubleValue();
        double numReceived  = received.doubleValue();
        return (numReceived/numRequested) * 100;
    }
    
    public boolean isSuccessRateGood() {
        // we want a large enough sample space.....
        if (requested.get() < sampleSize.get())
            return true;
        return (getSuccessRate() > MIN_SUCCESS_RATE);
    }
    
    public boolean isSuccessRateGreat() {
        // we want a large enough sample space.....
        if (requested.get() < sampleSize.get())
            return true;
        return (getSuccessRate() > PROXY_SUCCESS_RATE);
    }
    
    public boolean isSuccessRateTerrible() {
        // we want a large enough sample space.....
        if (requested.get() < sampleSize.get())
            return false;
        return (getSuccessRate() < TERRIBLE_SUCCESS_RATE);
    }
    
    public boolean isOOBEffectiveForProxy() {
        return !((sent.get() > 40) && (requested.get() == 0));
    }

    public boolean isOOBEffectiveForMe() {
        return !((sent.get() > 20) && (requested.get() == 0));
    }

}
