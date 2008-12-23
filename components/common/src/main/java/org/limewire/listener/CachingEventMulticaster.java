package org.limewire.listener;


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
public class CachingEventMulticaster<E> implements EventMulticaster<E>, EventBean<E> {
    
    private final EventListenerList<E> eventListenerList;
    private final BroadcastPolicy broadcastPolicy;
    private final Object LOCK = new Object();
    
    private volatile E cachedEvent;
    
    public CachingEventMulticaster() {
        this(BroadcastPolicy.ALWAYS);    
    }
    
    public CachingEventMulticaster(BroadcastPolicy broadcastPolicy) {
        eventListenerList = new EventListenerList<E>();
        this.broadcastPolicy = broadcastPolicy;
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
        eventListenerList.addListener(eEventListener);
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
        assert eventConsistentWithBroadcastPolicy(event);
        
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
            eventListenerList.broadcast(event);
        }
    }

    private boolean eventConsistentWithBroadcastPolicy(E event) {
        return broadcastPolicy == BroadcastPolicy.ALWAYS ||
                event.getClass().isEnum() ||
                System.identityHashCode(event) != event.hashCode(); // other case caching won't work
    }

    @Override
    public E getLastEvent() {
        return cachedEvent;
    }
}
