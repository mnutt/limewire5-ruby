package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 *  Paints a Green Message components.
 */
public class GreenMessagePainter<X> extends AbstractPainter<JXPanel> {

    /**
     * For creating buttons with different edge rounding properties
     * 
     *   Examples :     
     * 
     *       ( LEFT_ROUNDED |   | UNROUNDED |   | RIGHT_ROUNDED ) 
     */
    enum DrawMode {
        FULLY_ROUNDED, RIGHT_ROUNDED, LEFT_ROUNDED, UNROUNDED 
    }
    
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color backgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color backgroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color borderColour = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop1 = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop2 = PainterUtils.TRASPARENT;
    @Resource private Color bevelLeft = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color bevelBottom = PainterUtils.TRASPARENT;
    
    private Painter<JXPanel> normalPainter;
    
    private DrawMode drawMode = DrawMode.FULLY_ROUNDED;
    
    public GreenMessagePainter() {
        GuiUtils.assignResources(this);
        
        GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                0, 1, this.bevelRightGradientBottom, false);
        
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.setCacheable(false);
    }
    
    private Painter<JXPanel> createPainter(Color gradientTop, Color gradientBottom, 
            Paint border, Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom, int arcWidth, int arcHeight, AccentType accentType) {
        
        CompoundPainter<JXPanel> compoundPainter = new CompoundPainter<JXPanel>();
        
        RectanglePainter<JXPanel> painter = new RectanglePainter<JXPanel>();
        
        int shiftX1 = 0;
        int shiftX2 = 0;
        
        switch (this.drawMode) {       
            case LEFT_ROUNDED :
                shiftX1 = 0;
                shiftX2 = -arcWidth+2;
                break;
                
            case RIGHT_ROUNDED :
                shiftX1 = -arcWidth-2;
                shiftX2 = 0;
                break;
                
            case UNROUNDED :
                shiftX1 = -arcWidth-2;
                shiftX2 = -arcWidth-2;
                break;   
        }
        
        painter.setRounded(true);
        painter.setFillPaint(new GradientPaint(0,0, gradientTop, 0, 1, gradientBottom, false));
        painter.setRoundWidth(arcWidth);
        painter.setRoundHeight(arcHeight);
        painter.setInsets(new Insets(1,2+shiftX1,2,2+shiftX2));
        painter.setPaintStretched(true);
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
                
        BorderPainter borderPainter = new BorderPainter(arcWidth, arcHeight,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, accentType);
        borderPainter.setInsets(new Insets(0,shiftX1, 0, shiftX2));        
        
        compoundPainter.setPainters(painter, borderPainter);
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
        this.normalPainter.paint(g, object, width, height);
    }
}