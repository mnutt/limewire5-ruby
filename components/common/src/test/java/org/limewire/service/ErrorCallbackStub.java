package org.limewire.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple class that counts up how many exceptions it recieved.
 */
public class ErrorCallbackStub implements ErrorCallback {
    private final List<Throwable> exceptions = new CopyOnWriteArrayList<Throwable>();
    
    public void error(Throwable t) {
        exceptions.add(t);
    }
    
    public void error(Throwable t, String detail) {
        exceptions.add(t);
    }
    
    public int getExceptionCount() {
        return exceptions.size();
    }
    
    public Throwable getException(int i) {
        return exceptions.get(i);
    }
    
    public void clear() {
        exceptions.clear();
    }
}   
