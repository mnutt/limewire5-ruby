package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.ProgressBarForegroundPainter;
import org.limewire.ui.swing.painter.ProgressPainterFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeProgressBarFactory {
    
    private final ProgressPainterFactory painterFactory;
    
    @Inject
    LimeProgressBarFactory(ProgressPainterFactory painterFactory) {
        this.painterFactory = painterFactory;
    }
    
    
    public LimeProgressBar create() {
        AbstractPainter<JProgressBar> foregroundPainter = this.painterFactory.createRegularForegroundPainter();
        AbstractPainter<JComponent>   backgroundPainter = this.painterFactory.createRegularBackgroundPainter();
        
        LimeProgressBar bar = new LimeProgressBar(foregroundPainter, backgroundPainter);
        
        foregroundPainter.setCacheable(bar.hasCacheSupport());
        backgroundPainter.setCacheable(bar.hasCacheSupport());
        
        return bar;
    }
    
    public LimeProgressBar create(int min, int max) {
        LimeProgressBar bar = this.create();
        bar.setMinimum(min);
        bar.setMaximum(max);
        return bar;
    }
    
    public static LimeProgressBar createSplashProgressBar() {
        return new LimeProgressBar(
                new ProgressBarForegroundPainter<JProgressBar>(
                        new GradientPaint(0,0,new Color(0xcf,0xf8,0x8f),0,1,new Color(0xaa,0xcb,0x75)),
                        Color.GRAY),
                new RectanglePainter<JComponent>(Color.WHITE, new Color(0x27,0x27,0x27)));
    }
    
    public static LimeProgressBar createSplashProgressBarPro() {
        return new LimeProgressBar(
                new ProgressBarForegroundPainter<JProgressBar>(
                        new GradientPaint(0,0,new Color(0x3883cf),0,1,new Color(0x1667a8)),
                        Color.GRAY, new Color(0x3d9dff)),
                new RectanglePainter<JComponent>(Color.WHITE, new Color(0x00316b)));
    }
}
