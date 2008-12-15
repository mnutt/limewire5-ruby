package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.ui.swing.library.Disposable;

public interface ShareWidget<T> extends Disposable{

    public void show(Component c);

    public void setShareable(T item);

}
