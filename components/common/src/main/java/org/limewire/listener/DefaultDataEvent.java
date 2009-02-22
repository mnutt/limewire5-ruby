package org.limewire.listener;

import org.limewire.util.Objects;

public class DefaultDataEvent<D> implements DataEvent<D> {
    
    private final D data;
    
    public DefaultDataEvent(D data) {
        this.data = Objects.nonNull(data, "data");
    }
    
    public D getData() {
        return data;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + data.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(!obj.getClass().equals(getClass())) {
            return false;
        }
        return data.equals(((DefaultDataEvent)obj).getData());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " - data: " + data;
    }
}
