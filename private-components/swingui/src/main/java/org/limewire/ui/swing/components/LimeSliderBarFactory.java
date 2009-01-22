package org.limewire.ui.swing.components;

import org.limewire.ui.swing.painter.SliderPainterFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeSliderBarFactory {

    private final SliderPainterFactory painterFactory;
    
    @Inject
    public LimeSliderBarFactory(SliderPainterFactory barPainterFactory) {
        this.painterFactory = barPainterFactory;
    }
    
    public LimeSliderBar create() {
        return new LimeSliderBar(painterFactory.createMediaForegroundPainter(),
                painterFactory.createMediaBackgroundPainter());
    }
}
