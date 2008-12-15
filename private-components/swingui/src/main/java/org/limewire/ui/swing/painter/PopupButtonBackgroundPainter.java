package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

/**
 * Background painter for a gradient button. 
 */

@Singleton
public class PopupButtonBackgroundPainter extends AbstractPainter<JXButton> {
    
    @Resource
    private int arcWidth;
    @Resource
    private Color backgroundPressed;
    @Resource
    private Color backgroundRollover;
    
    public PopupButtonBackgroundPainter() {
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(true);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        if (button.getModel().isPressed()) {
            g.setColor(this.backgroundPressed);
            g.fillRoundRect(0, 0, width-1, height-1, arcWidth, height-1);
        }
        else if (button.getModel().isRollover()) {
            g.setColor(this.backgroundRollover);
            g.fillRoundRect(0, 0, width-1, height-1, arcWidth, height-1);
        }
    }
}