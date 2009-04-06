package org.limewire.inject;

import com.google.inject.Provider;

/** An extension to {@link Provider} that allows the value to change. */
public interface MutableProvider<T> extends Provider<T> {
    
    /** Sets the new value that this will provide. */
    public void set(T newValue);

}
