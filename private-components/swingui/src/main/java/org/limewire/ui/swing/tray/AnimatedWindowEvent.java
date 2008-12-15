package org.limewire.ui.swing.tray;

import java.util.EventObject;

import org.limewire.ui.swing.tray.AnimatedWindow.AnimationType;


class AnimatedWindowEvent extends EventObject {

    private final AnimationType animationType;
    
    public AnimatedWindowEvent(Object source, AnimationType animationType) {
        super(source);
        
        this.animationType = animationType;
    }

    public AnimationType getAnimationType() {
        return animationType;
    }
    
}
