package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Paints a rounded border like box with 
 *  one pixel inner shadowing/beveling
 */

public class BorderPainter<X> extends AbstractPainter<X> {

    private final int arcWidth;
    private final int arcHeight;
    private final Paint border;
    private final Paint bevelLeft;
    private final Paint bevelTop1;
    private final Paint bevelTop2;
    private final Paint bevelRight;
    private final Paint bevelBottom;
    
    private final Paint accentPaint1;
    private final Paint accentPaint2;
    private final Paint accentPaint3;
    
    private Insets insets = PainterUtils.BLANK_INSETS;
    
    private final AccentType accentType;
    
    private static final Paint BUBBLE_PAINT1 = new Color(0xee,0xee,0xee);
    private static final Paint BUBBLE_PAINT2 = new Color(0xed,0xed,0xed);
    private static final Paint BUBBLE_PAINT3 = new Color(0xf0,0xf0,0xf0);
    
    private static final Paint SHADOW_PAINT1 = new Color(0x5f,0x5f,0x5f);
    private static final Paint SHADOW_PAINT2 = new Color(0x5e,0x5e,0x5e);
    private static final Paint SHADOW_PAINT3 = new Color(0x64,0x64,0x64);
    
    private static final Paint GREEN_SHADOW_PAINT1 = new Color(0xc3d9a1);
    private static final Paint GREEN_SHADOW_PAINT2 = new Color(0xb9d78d);
    private static final Paint GREEN_SHADOW_PAINT3 = new Color(0xe1eecc);
    
    public BorderPainter(int arcWidth, int arcHeight, Paint border, 
            Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom, AccentType accentType) {
        
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.border = border;
        this.bevelLeft = bevelLeft;
        this.bevelTop1 = bevelTop1;
        this.bevelTop2 = bevelTop2;
        this.bevelRight = bevelRight;
        this.bevelBottom = bevelBottom;
        
        this.accentType = accentType;
        
        switch (accentType) {
        
        case BUBBLE :
            
            accentPaint1 = BUBBLE_PAINT1;
            accentPaint2 = BUBBLE_PAINT2;
            accentPaint3 = BUBBLE_PAINT3;
            break;
            
        case SHADOW :
            
            accentPaint1 = SHADOW_PAINT1;
            accentPaint2 = SHADOW_PAINT2;
            accentPaint3 = SHADOW_PAINT3;
            break;
            
        case GREEN_SHADOW :    
            
            accentPaint1 = GREEN_SHADOW_PAINT1;
            accentPaint2 = GREEN_SHADOW_PAINT2;
            accentPaint3 = GREEN_SHADOW_PAINT3;
            break;
            
        default:
        
            accentPaint1 = PainterUtils.TRASPARENT;
            accentPaint2 = PainterUtils.TRASPARENT;
            accentPaint3 = PainterUtils.TRASPARENT;
            
        }
        
        this.setCacheable(true);
    }

    public int getArcHeight() {
        return arcHeight;
    }

    public int getArcWidth() {
        return arcWidth;
    }
    
    public void setInsets(Insets insets) {
        this.insets = insets;
    }
    
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        
        int ix1 = this.insets.left;
        int ix2 = this.insets.right;
        
        int singleArcHeight = this.arcHeight/2;
        
        // Draw upper bevels
        g.setClip(0+ix1, 0, width-2-ix1-ix2, 7);
        g.setPaint(this.bevelTop2);
        g.drawRoundRect(1+ix1, 2, width-2-ix1-ix2, height-5, this.arcWidth, this.arcHeight);
        g.setPaint(this.bevelTop1);
        g.drawRoundRect(1+ix1, 1, width-3-ix1-ix2, height-4, this.arcWidth, this.arcHeight);
        
        // Draw side and bottom bevels
        g.setClip(0+ix1, singleArcHeight, width-2-ix1-ix2, height);
        g.setPaint(this.bevelBottom);        
        g.drawRoundRect(1+ix1, 1, width-4-ix1-ix2, height-4, this.arcWidth, this.arcHeight);
        g.setClip(0+ix1, singleArcHeight-1, width-2-ix1-ix2, height);
        g.setPaint(PaintUtils.resizeGradient(this.bevelLeft, 0, height-singleArcHeight+1));
        g.drawLine(2+ix1,singleArcHeight-1,2+ix1,height-singleArcHeight);
        g.setPaint(PaintUtils.resizeGradient(this.bevelRight, 0, height-singleArcHeight+1));
        g.drawLine(width-3-ix2,singleArcHeight-1,width-3-ix2,height-singleArcHeight);
                
        
        if (this.accentType != AccentType.NONE) {
            // Draw the bottom accent bubble or shadow
        
            g.setClip(0+ix1, singleArcHeight, width-ix1-ix2, height);
            g.setPaint(accentPaint3);
            g.drawRoundRect(0+ix1, 0, width-1-ix1-ix2, height-1, this.arcWidth, this.arcHeight);
            g.setPaint(accentPaint2);        
            g.drawLine(0+ix1,singleArcHeight,0+ix1,height/2);
            g.drawLine(width-1-ix2,singleArcHeight,width-1-ix2,height/2);
            g.setPaint(accentPaint1);
            g.drawLine(0+ix1,height/2,0+ix1,height-singleArcHeight);
            g.drawLine(width-1-ix2,height/2,width-1-ix2,height-singleArcHeight);
        }
         
        g.setClip(0+ix1, 0, width-ix1-ix2, height);
        
        // Draw final border
        g.setPaint(PaintUtils.resizeGradient(this.border, 0, height));
        g.drawRoundRect(1+ix1, 0, width-3-ix1-ix2, height-2, this.arcWidth, this.arcHeight);
        
        // Cap the left border if it is not rounded on the left        
        if (ix1 < 0) {
            g.drawLine(0, 1, 0, height-2);
        }

        // Cap the right border if it is not rounded on the right        
        if (ix2 < 0) {
            GradientPaint spanGradient 
            = new GradientPaint(0,1, PainterUtils.getColour(this.bevelTop1), 
                    0, height-3, PainterUtils.getColour(this.bevelBottom), false);
            
            g.setPaint(spanGradient);
            g.drawLine(width-1, 2, width-1, height-3);
        }
    }
    
    public enum AccentType {
        SHADOW, GREEN_SHADOW, BUBBLE, NONE
    }

}
