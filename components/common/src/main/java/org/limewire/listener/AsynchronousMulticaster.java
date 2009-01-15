package org.limewire.listener;

import java.util.concurrent.Executor;

import org.limewire.logging.Log;

/**
 * An EventMulticaster that broadcasts all its events within an executor.
 * This is useful if you want to asynchronously notify listeners about events
 * (especially handy if the events are generated while holding a lock).
 * If the executor is single-threaded, this can guarantee that all events
 * are dispatched in the same order they were broadcast.
 */
public class AsynchronousMulticaster<E> implements EventMulticaster<E> {
    
    private final EventListenerList<E> listeners;
    private final Executor executor;
    
    public AsynchronousMulticaster(Executor executor) {
        this.listeners = new EventListenerList<E>();
        this.executor = executor;
    }
    
    public AsynchronousMulticaster(Executor executor, Class loggerKey) {
        this.listeners = new EventListenerList<E>(loggerKey);
        this.executor = executor;
    }
    
    public AsynchronousMulticaster(Executor executor, Log log) {
        this.listeners = new EventListenerList<E>(log);
        this.executor = executor;
    }

    @Override
    public void addListener(EventListener<E> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<E> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }

    @Override
    public void broadcast(final E event) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listeners.broadcast(event);
            }
        });
    }

}
