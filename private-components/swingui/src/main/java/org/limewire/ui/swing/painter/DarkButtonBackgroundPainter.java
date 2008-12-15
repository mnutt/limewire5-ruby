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
public class DarkButtonBackgroundPainter extends ButtonBackgroundPainter {
        
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    
    @Resource private Color border = PainterUtils.TRASPARENT;
    
    @Resource private Color normalGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color normalGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color normalBevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color normalBevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color normalBevelBottom = PainterUtils.TRASPARENT;
    
    @Resource private Color highlightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color highlightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color highlightBevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color highlightBevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color highlightBevelBottom = PainterUtils.TRASPARENT;
    
    @Resource private Color clickGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color clickGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color clickBevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color clickBevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color clickBevelBottom = PainterUtils.TRASPARENT;
    
    @Resource private Color disabledGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color disabledGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color disabledBorderGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color disabledBorderGradientBottom = PainterUtils.TRASPARENT;
    
   
    public DarkButtonBackgroundPainter(DrawMode mode, AccentType accentType) {
        GuiUtils.assignResources(this);
        
        this.drawMode = mode;
        
        GradientPaint normalRightGradient = new GradientPaint(0,0, this.normalBevelRightGradientTop, 
                0, 1, this.normalBevelRightGradientBottom, false);
        
        GradientPaint hoveredRightGradient = new GradientPaint(0,0, this.highlightBevelRightGradientTop, 
                0, 1, this.highlightBevelRightGradientBottom, false);
        
        GradientPaint clickedRightGradient = new GradientPaint(0,0, this.clickBevelRightGradientTop, 
                0, 1, this.clickBevelRightGradientBottom, false);
        
        GradientPaint disabledBorderGradient = new GradientPaint(0,0, this.disabledBorderGradientTop, 
                0, 1, this.disabledBorderGradientBottom, false);
        
        this.normalPainter = createPainter(this.normalGradientTop, this.normalGradientBottom,
                this.border, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT,
                normalRightGradient, this.normalBevelBottom, this.arcWidth, this.arcHeight, accentType);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.border,  PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
                hoveredRightGradient, this.highlightBevelBottom, this.arcWidth, this.arcHeight, accentType);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.border,  PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
                clickedRightGradient, this.clickBevelBottom, this.arcWidth, this.arcHeight, accentType);
        
        this.disabledPainter = createPainter(this.disabledGradientTop, this.disabledGradientBottom,
                disabledBorderGradient,  PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
                PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, this.arcWidth, this.arcHeight, accentType);
        
        this.setCacheable(false);
    }
}