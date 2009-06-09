package org.limewire.ui.swing.library.nav;

import javax.swing.JComponent;

public interface NavMediator<T extends JComponent> {

    /** Returns the component of this mediator. */
    public T getComponent();
}
