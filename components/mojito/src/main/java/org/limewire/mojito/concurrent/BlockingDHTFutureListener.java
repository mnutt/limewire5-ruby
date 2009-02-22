package org.limewire.mojito.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.OnewayExchanger;

public class BlockingDHTFutureListener<T> implements DHTFutureListener<T> {
    
    private final OnewayExchanger<T, ExecutionException> exchanger = new OnewayExchanger<T, ExecutionException>();
    
    @Override
    public void handleCancellationException(CancellationException e) {
        exchanger.cancel();
    }

    @Override
    public void handleExecutionException(ExecutionException e) {
        exchanger.setException(e);
    }
    
    @Override
    public void handleFutureSuccess(T result) {
        exchanger.setValue(result);
    }
    
    @Override
    public void handleInterruptedException(InterruptedException e) {
        exchanger.cancel();
    }

    public T get() throws ExecutionException, InterruptedException {
        return exchanger.get();
    }
    
    public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
        return exchanger.get(timeout, unit);
    }
    
}
