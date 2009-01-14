package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a Painter for a floating message panel. This has a drop shadow
 * effect to help the illusion the message is floating above the
 * containing component.
 */
public class BackgroundMessagePainter<X> extends AbstractPainter<JXPanel> {

    @Resource
    private Color messageBorder;
    @Resource
    private Color messageShadowColor;
    @Resource
    private Color messageBackgroundColor;
    
    private ShadowPathEffect shadow;
    private int BORDER_INSETS = 5;
    private int arc = 10;
    
    public BackgroundMessagePainter() {
        GuiUtils.assignResources(this);
        
        shadow = new ShadowPathEffect();
        shadow.setEffectWidth(14);
        shadow.setBrushColor(messageShadowColor);
        shadow.setOffset(new Point(0, 0));
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
        RoundRectangle2D.Float panelShape = new RoundRectangle2D.Float(BORDER_INSETS, BORDER_INSETS,width - 2 * BORDER_INSETS,height - 2 * BORDER_INSETS, arc, arc);
        Area panelArea = new Area(panelShape);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        
        g2.setColor(messageBackgroundColor);
        g2.fill(panelArea);        

        g2.setColor(messageBorder);
        g2.draw(panelShape);
        
        shadow.apply(g2, panelShape, width, height);
        g2.dispose();         
    }
}
