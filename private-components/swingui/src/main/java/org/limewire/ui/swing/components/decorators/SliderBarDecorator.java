package org.limewire.ui.swing.components.decorators;

import org.limewire.ui.swing.components.LimeSliderBar;
import org.limewire.ui.swing.painter.factories.SliderPainterFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SliderBarDecorator {

    private final SliderPainterFactory painterFactory;
    
    @Inject
    public SliderBarDecorator(SliderPainterFactory barPainterFactory) {
        this.painterFactory = barPainterFactory;
    }
    
    public void decoratePlain(LimeSliderBar bar) {
        bar.setForegroundPainter(painterFactory.createMediaForegroundPainter());
        bar.setBackgroundPainter(painterFactory.createMediaBackgroundPainter());
    }
}
