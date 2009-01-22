package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

public class StatusBarSectionPainter<X> extends AbstractPainter<X>{

    @Resource private Color dividerForeground;
    
    public StatusBarSectionPainter() {
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(false);
        this.setCacheable(true);
    }

    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        g.setPaint(this.dividerForeground);
        g.drawLine(0, 2, 0, height-3);
    }    
}
