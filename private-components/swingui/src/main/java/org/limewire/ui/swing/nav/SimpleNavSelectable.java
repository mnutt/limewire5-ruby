package org.limewire.ui.swing.nav;

public class SimpleNavSelectable<T> implements NavSelectable<T> {

    private final T t;
    
    public static <T> SimpleNavSelectable<T> create(T t) {
        return new SimpleNavSelectable<T>(t);
    }
    
    public SimpleNavSelectable(T t) {
        this.t = t;
    }
    
    @Override
    public T getNavSelectionId() {
        return t;
    }
    
}
