package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.ProgressBarForegroundPainter;
import org.limewire.ui.swing.painter.ProgressBarPainterFactory;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;

@Singleton
public class LimeProgressBarFactory {
    
    private final ProgressBarPainterFactory painterFactory;
    
    @Inject
    LimeProgressBarFactory(ProgressBarPainterFactory painterFactory) {
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
                new RectanglePainter<JComponent>(Color.WHITE, new Color(0x53,0x7e,0x10)));
    }
    
    
    public static void main(String[] args) {
        JFrame a = new JFrame();
        JPanel p = new JPanel(new FlowLayout());
        
        Injector injector = Guice.createInjector(new Module() {
            public void configure(Binder binder) {
                binder.bind(LimeProgressBarFactory.class);
            }
            
        });
        
        LimeProgressBarFactory factory = injector.getInstance(LimeProgressBarFactory.class);
        LimeProgressBar ppp1 = factory.create();
        LimeProgressBar ppp2 = factory.create();
        
        ppp2.setEnabled(false);
        
        ppp1.setValue(50);
        ppp2.setValue(50);
        
        p.add(ppp1);
        p.add(ppp2);
        a.add(p);
        a.pack();
        a.setVisible(true);
    }
}
