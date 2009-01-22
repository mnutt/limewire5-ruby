package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JSlider;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class SliderPainterFactory {

    @Resource private Color mediaSliderBorder = PainterUtils.TRASPARENT;
    @Resource private Color mediaSliderBackgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color mediaSliderBackgroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color mediaSliderForegroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color mediaSliderForegroundGradientBottom = PainterUtils.TRASPARENT;
    
    public SliderPainterFactory() { 
        GuiUtils.assignResources(this);
    }
    
    public AbstractPainter<JComponent> createMediaBackgroundPainter() {
        return new ProgressBarBackgroundPainter(
                new GradientPaint(0,0,mediaSliderBackgroundGradientTop,0,1,mediaSliderBackgroundGradientBottom),
                mediaSliderBorder, mediaSliderBorder);
    }
    
    public AbstractPainter<JSlider> createMediaForegroundPainter() {
        
        ProgressBarForegroundPainter<JSlider> painter =  new ProgressBarForegroundPainter<JSlider>(
             new GradientPaint(0,0,mediaSliderForegroundGradientTop,0,1,mediaSliderForegroundGradientBottom),
             Color.GRAY, true);
        
        painter.setCacheable(false);
        
        return painter;
    }
    
}
