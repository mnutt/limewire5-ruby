package org.limewire.listener;

import org.limewire.logging.Log;


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
public class CachingEventMulticasterImpl<E> implements CachingEventMulticaster<E> {
    
    private final EventMulticaster<E> multicaster;
    private final BroadcastPolicy broadcastPolicy;
    private final Object LOCK = new Object();
    
    private volatile E cachedEvent;
    
    public CachingEventMulticasterImpl() {
        this(BroadcastPolicy.ALWAYS, new EventMulticasterImpl<E>());    
    }
    
    public CachingEventMulticasterImpl(Log log) {
        this(BroadcastPolicy.ALWAYS, log);
    }
    
    public CachingEventMulticasterImpl(BroadcastPolicy broadcastPolicy) {
        this(broadcastPolicy, new EventMulticasterImpl<E>());
    }
    
    public CachingEventMulticasterImpl(BroadcastPolicy broadcastPolicy, Log log) {
        this(broadcastPolicy, new EventMulticasterImpl<E>(log));
    }
    
    public CachingEventMulticasterImpl(BroadcastPolicy broadcastPolicy, EventMulticaster<E> multicaster) {
        this.broadcastPolicy = broadcastPolicy;
        this.multicaster = multicaster;
    }

    @Override
    /**
     * Adds a listener and calls its handleEvent() method with the
     * most recent Event, if any.
     */
    public void addListener(EventListener<E> eEventListener) {
        if(cachedEvent != null) {
            EventListenerList.dispatch(eEventListener, cachedEvent);
        }
        multicaster.addListener(eEventListener);
    }

    @Override
    public boolean removeListener(EventListener<E> eEventListener) {
        return multicaster.removeListener(eEventListener);
    }

    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }

    @Override
    public void broadcast(E event) {        
        boolean broadcast = false;
        synchronized(LOCK) {
            if(cachedEvent == null ||
                    broadcastPolicy == BroadcastPolicy.ALWAYS ||
                    !cachedEvent.equals(event)) {
                cachedEvent = event;
                broadcast = true;             
            }
        }
        
        if(broadcast) {
            multicaster.broadcast(event);
        }
    }

    @Override
    public E getLastEvent() {
        return cachedEvent;
    }
}