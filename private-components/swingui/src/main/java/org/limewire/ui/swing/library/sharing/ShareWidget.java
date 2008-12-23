package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.ui.swing.components.Disposable;

public interface ShareWidget<T> extends Disposable{

    /**
     * @param owner the widget is shown positioned relative to this component.  It is centered in the frame if owner is null;
     */
    public void show(Component owner);

    public void setShareable(T item);

}
