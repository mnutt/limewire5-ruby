package org.limewire.listener;

import org.limewire.util.Objects;

public class AbstractSourcedEvent<S> {
    
    private final S source;
    
    public AbstractSourcedEvent(S source) {
        this.source = Objects.nonNull(source, "source");
    }
    
    public S getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + source.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(!obj.getClass().equals(getClass())) {
            return false;
        }
        return source.equals(((AbstractSourcedEvent)obj).getSource());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " - source: " + source;
    }
}
