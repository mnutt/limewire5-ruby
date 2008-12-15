package org.limewire.listener;

/**
 * An event for a given source and type.
 */
public interface Event<S, T> extends SourcedEvent<S>, TypedEvent<T> {

}
