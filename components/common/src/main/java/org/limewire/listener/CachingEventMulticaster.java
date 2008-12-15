package org.limewire.listener;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A Multicaster that caches the last event it handles or broadcasts.
 *
 * The cached event is used for two purposes:
 *
 * 1. New listeners who are added will have their handleEvent(E event)
 * method called with the cached event
 *
 * 2. When broadcast(E event) and handleEvent(E event) are called
 * on the CacheingEventMulticaster, the event is only broadcast
 * if it is not equal to the cached event.  Event classes should override equals()
 * for this to provide any meaningful implementation
 *
 * @param <E>
 */
public class CachingEventMulticaster<E> implements EventMulticaster<E> {
    
    private final EventListenerList<E> eventListenerList;    
    private final ReadWriteLock eventLock;
    
    private volatile E cachedEvent;
    
    public CachingEventMulticaster() {
        eventListenerList = new EventListenerList<E>();
        eventLock = new ReentrantReadWriteLock();
    }

    @Override
    /**
     * Adds a listener and calls its handleEvent() method with the
     * most recent Event, if any.
     */
    public void addListener(EventListener<E> eEventListener) {
        boolean broadcast = false;
        eventLock.readLock().lock();        
        try {
            if(cachedEvent != null) {
                broadcast = true;
            }
            eventListenerList.addListener(eEventListener);
        } finally {
            eventLock.readLock().unlock();
        }
        // race condtion here
        if(broadcast) {
            EventListenerList.dispatch(eEventListener, cachedEvent);
        }
    }

    @Override
    public boolean removeListener(EventListener<E> eEventListener) {
        return eventListenerList.removeListener(eEventListener);
    }

    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }

    @Override
    public void broadcast(E event) {
        // This fails because we're broadcasting enums.
//        assert System.identityHashCode(event) != event.hashCode(); // otherwise caching won't work
        boolean broadcast = false;
        eventLock.writeLock().lock();        
        try {
            if(cachedEvent == null || !cachedEvent.equals(event)) {
                cachedEvent = event;
                broadcast = true;             
            }
        } finally {
            eventLock.writeLock().unlock();
        }
        
        if(broadcast) {
            eventListenerList.broadcast(event);
        }
    }
}
