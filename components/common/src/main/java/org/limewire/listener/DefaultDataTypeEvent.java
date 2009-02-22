package org.limewire.listener;

import org.limewire.util.Objects;

/**
 * A default, simple implementation of Event.
 */
public class DefaultDataTypeEvent<D, E> extends DefaultDataEvent<D> implements DataTypeEvent<D, E> {
    
    private final E event;
    
    public DefaultDataTypeEvent(D data, E event) {
        super(data);
        this.event = Objects.nonNull(event, "event");
    }

    public E getType() {
        return event;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + event.hashCode();
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
        return event.equals(((DefaultDataTypeEvent)obj).getType());
    }

    @Override
    public String toString() {
        return super.toString() + ", event: " + event;
    }
}
