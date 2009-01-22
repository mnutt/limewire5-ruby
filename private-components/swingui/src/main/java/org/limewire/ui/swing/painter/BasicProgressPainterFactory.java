package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Singleton;

@Singleton
public class BasicProgressPainterFactory implements ProgressPainterFactory {
    
    @Resource private Color barBorder = PainterUtils.TRASPARENT;
    @Resource private Color barBorderDisabled = PainterUtils.TRASPARENT;
    @Resource private Color barBackgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color barBackgroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color barForegroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color barForegroundGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color barDisabledForegroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color barDisabledForegroundGradientBottom = PainterUtils.TRASPARENT;
    
    public BasicProgressPainterFactory() { 
        GuiUtils.assignResources(this);
    }
    
    @Override
    public AbstractPainter<JComponent> createRegularBackgroundPainter() {
        return new ProgressBarBackgroundPainter(
             new GradientPaint(0,0,this.barBackgroundGradientTop,0,1,this.barBackgroundGradientBottom),
             this.barBorder, this.barBorderDisabled);
    }
    
    @Override
    public AbstractPainter<JProgressBar> createRegularForegroundPainter() {
        return new ProgressBarForegroundPainter<JProgressBar>(
             new GradientPaint(0,0,this.barForegroundGradientTop,0,1,this.barForegroundGradientBottom),
             new GradientPaint(0,0,this.barDisabledForegroundGradientTop,0,1,this.barDisabledForegroundGradientBottom));
    }
}
