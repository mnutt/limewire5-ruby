package org.limewire.listener;

/**
 * An implementation of an event multicaster.
 * This forwards all received events to all listeners.
 */
public class EventMulticasterImpl<E> implements EventMulticaster<E> {

    private final EventListenerList<E> listeners = new EventListenerList<E>();
    
    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }
    
    @Override
    public void broadcast(E event) {
        listeners.broadcast(event);
    }

    @Override
    public void addListener(EventListener<E> eventListener) {
        listeners.addListener(eventListener);
    }

    @Override
    public boolean removeListener(EventListener<E> eventListener) {
        return listeners.removeListener(eventListener);        
    }
}
