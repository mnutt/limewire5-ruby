package org.limewire.ui.swing.components;

import org.limewire.ui.swing.painter.ProgressBarPainterFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeSliderBarFactory {

    private final ProgressBarPainterFactory barPainterFactory;
    
    @Inject
    public LimeSliderBarFactory(ProgressBarPainterFactory barPainterFactory) {
        this.barPainterFactory = barPainterFactory;
    }
    
    public LimeSliderBar create() {
        return new LimeSliderBar(barPainterFactory.createMediaBarForegroundPainter(),
                barPainterFactory.createMediaBarBackgroundPainter());
    }
}
