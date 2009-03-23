package com.limegroup.gnutella.stubs;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorServiceStub extends AbstractExecutorService implements ScheduledExecutorService {

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isShutdown() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isTerminated() {
        // TODO Auto-generated method stub
        return false;
    }

    public void shutdown() {
        // TODO Auto-generated method stub
        
    }

    public List<Runnable> shutdownNow() {
        // TODO Auto-generated method stub
        return null;
    }

    public void execute(Runnable command) {
        // TODO Auto-generated method stub
        
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay,
            TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
            TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
            long initialDelay, long period, TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
            long initialDelay, long delay, TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
