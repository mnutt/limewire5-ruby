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
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + (data != null ? data.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) {
            return false;
        }
        if(!obj.getClass().equals(getClass())) {
            return false;
        }
        return data.equals(((DefaultDataEvent)obj).getData());
    }
    
    @Override
    public String toString() {
        return super.toString() + ", data: " + data;
    }

}
