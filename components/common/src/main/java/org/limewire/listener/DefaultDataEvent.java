package org.limewire.listener;

/**
 * Default class for events that can carry data in addition to a source
 * and an event type. 
 */
public class DefaultDataEvent<S, E, D> extends DefaultEvent<S, E> {

    private final D data;

    public DefaultDataEvent(S source, E event, D data) {
        super(source, event);
        this.data = data;
    }
    
    public D getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return super.toString() + ", data: " + data;
    }

}
