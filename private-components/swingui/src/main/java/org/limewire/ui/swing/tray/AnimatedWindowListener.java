package org.limewire.ui.swing.tray;

import java.util.EventListener;

interface AnimatedWindowListener extends EventListener {

    void animationStarted(AnimatedWindowEvent event);
    
    void animationStopped(AnimatedWindowEvent event);

    void animationCompleted(AnimatedWindowEvent event);
    
}
