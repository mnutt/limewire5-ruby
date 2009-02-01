package org.limewire.ui.swing.painter;

import org.limewire.ui.swing.painter.factories.BasicProgressPainterFactory;
import org.limewire.ui.swing.painter.factories.ProProgressPainterFactory;
import org.limewire.ui.swing.painter.factories.ProgressPainterFactory;

import com.google.inject.AbstractModule;

public class LimeWireUiPainterModule extends AbstractModule {

    private final boolean isPro;
    
    public LimeWireUiPainterModule(boolean isPro) {
        this.isPro = isPro;
    }

    @Override
    protected void configure() {
        if (isPro) {
            bind(ProgressPainterFactory.class).to(ProProgressPainterFactory.class);
        } else {
            bind(ProgressPainterFactory.class).to(BasicProgressPainterFactory.class);
        }
    }

}
