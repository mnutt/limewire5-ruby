package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Background painter for a gradient button. 
 */
public class GreenButtonBackgroundPainter extends ButtonBackgroundPainter {
        
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color backgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color backgroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color highlightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color highlightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color clickGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color clickGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color border = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop1 = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop2 = PainterUtils.TRASPARENT;
    @Resource private Color bevelLeft = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color bevelBottom = PainterUtils.TRASPARENT;
    
    public GreenButtonBackgroundPainter() {
        GuiUtils.assignResources(this);
                
        GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                0, 1, this.bevelRightGradientBottom, false);
        
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.border, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.border, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.border, bevelLeft, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.setCacheable(false);
    }
}