package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class ProgressBarPainterFactory {

    @Resource private Color barBorder;
    @Resource private Color barBorderDisabled;
    @Resource private Color barBackgroundGradientTop;
    @Resource private Color barBackgroundGradientBottom;
    @Resource private Color barForegroundGradientTop;
    @Resource private Color barForegroundGradientBottom;
    @Resource private Color barDisabledForegroundGradientTop;
    @Resource private Color barDisabledForegroundGradientBottom;
    
    @Resource private Color mediaBarBorder;
    @Resource private Color mediaBarBackgroundGradientTop;
    @Resource private Color mediaBarBackgroundGradientBottom;
    @Resource private Color mediaBarForegroundGradientTop;
    @Resource private Color mediaBarForegroundGradientBottom;
    
    public ProgressBarPainterFactory() { 
        GuiUtils.assignResources(this);
    }
    
    public AbstractPainter<JComponent> createRegularBackgroundPainter() {
        return new ProgressBarBackgroundPainter(
             new GradientPaint(0,0,this.barBackgroundGradientTop,0,1,this.barBackgroundGradientBottom),
             this.barBorder, this.barBorderDisabled);
    }
    
    public AbstractPainter<JProgressBar> createRegularForegroundPainter() {
        return new ProgressBarForegroundPainter<JProgressBar>(
             new GradientPaint(0,0,this.barForegroundGradientTop,0,1,this.barForegroundGradientBottom),
             new GradientPaint(0,0,this.barDisabledForegroundGradientTop,0,1,this.barDisabledForegroundGradientBottom));
    }
    
    public AbstractPainter<JComponent> createMediaBarBackgroundPainter() {
        return new ProgressBarBackgroundPainter(
                new GradientPaint(0,0,mediaBarBackgroundGradientTop,0,1,mediaBarBackgroundGradientBottom),
                mediaBarBorder, mediaBarBorder);
    }
    
    public AbstractPainter<JSlider> createMediaBarForegroundPainter() {
        
        ProgressBarForegroundPainter<JSlider> painter =  new ProgressBarForegroundPainter<JSlider>(
             new GradientPaint(0,0,mediaBarForegroundGradientTop,0,1,mediaBarForegroundGradientBottom),
             Color.GRAY, true);
        
        painter.setCacheable(false);
        
        return painter;
    }
}
