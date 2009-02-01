package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 *  Paints a Green Message component.
 */
public class GreenMessagePainter<X> extends CompoundPainter<X> {

    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color backgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color backgroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color border = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop1 = PainterUtils.TRASPARENT;
    @Resource private Color bevelTop2 = PainterUtils.TRASPARENT;
    @Resource private Color bevelLeft = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color bevelRightGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color bevelBottom = PainterUtils.TRASPARENT;
        
    public GreenMessagePainter() {

        GuiUtils.assignResources(this);
        
        Paint background = new GradientPaint(0,0, backgroundGradientTop,
                                             0,1, backgroundGradientBottom);
        
        Paint bevelRight = new GradientPaint(0,0, bevelRightGradientTop,
                                             0,1, bevelRightGradientBottom);
        
        setPainters(new ComponentBackgroundPainter<X>(background, border, bevelLeft, bevelTop1, bevelTop2,
                bevelRight, bevelBottom, arcWidth, arcHeight, AccentType.NONE));
        
        setCacheable(true);
    }
    

}