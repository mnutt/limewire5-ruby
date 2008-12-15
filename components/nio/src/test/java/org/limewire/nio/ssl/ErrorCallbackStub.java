package org.limewire.nio.ssl;

import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.service.ErrorCallback;

/**
 * Simple class that counts up how many exceptions it recieved.
 */
public class ErrorCallbackStub implements ErrorCallback {
    private final AtomicInteger exceptions = new AtomicInteger(0);
    private volatile Throwable caught;
    
    public void error(Throwable t) {
        exceptions.getAndIncrement();
        caught = t;
    }
    
    public Throwable getLastCaught() {
        return caught;
    }
    
    public void error(Throwable t, String detail) {
        exceptions.getAndIncrement();
        caught = t;
    }
    
    public int getExceptionCount() {
        return exceptions.get();
    }
}   
