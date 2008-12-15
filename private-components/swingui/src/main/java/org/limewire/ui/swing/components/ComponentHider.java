package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * This mouse listener is intended to hide the supplied component after the mouse
 * has left the scope of all JComponents this this instance has been added as a 
 * listener to. 
 */
public final class ComponentHider extends MouseAdapter {
    private final JComponent component;
    private final AdditionalBehavior additionalBehavior;
    private final Timer hideTimer = new Timer(200, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (component.isVisible()) {
                    component.setVisible(false);
                    if (additionalBehavior != null) {
                        additionalBehavior.componentHidden();
                    }
                }
            } finally {
                ((Timer)e.getSource()).stop();
            }
        }
    });
    
    public ComponentHider(JComponent menu) {
        this(menu, null);
    }

    public ComponentHider(JComponent menu, AdditionalBehavior additionalBehavior) {
        this.component = menu;
        this.additionalBehavior = additionalBehavior;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hideTimer.stop();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hideTimer.start();
    }
    
    /**
     * Callback to allow additional behavior to happen after the component has been hidden
     */
    public static interface AdditionalBehavior {
        void componentHidden();
    }
}