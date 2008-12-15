package org.limewire.listener;

public interface SourcedEventMulticaster<E extends SourcedEvent<S>, S> extends
        SourcedListenerSupport<E, S>, ListenerSupport<E>, EventListener<E>, EventBroadcaster<E> {
    
}
