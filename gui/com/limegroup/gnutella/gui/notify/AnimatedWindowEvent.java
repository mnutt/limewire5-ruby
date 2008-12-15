package com.limegroup.gnutella.gui.notify;

import java.util.EventObject;

import com.limegroup.gnutella.gui.notify.AnimatedWindow.AnimationType;

public class AnimatedWindowEvent extends EventObject {

    private final AnimationType animationType;
    
    public AnimatedWindowEvent(Object source, AnimationType animationType) {
        super(source);
        
        this.animationType = animationType;
    }

    public AnimationType getAnimationType() {
        return animationType;
    }
    
}
