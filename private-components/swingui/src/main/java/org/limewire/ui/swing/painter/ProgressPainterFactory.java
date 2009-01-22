package org.limewire.ui.swing.painter;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;

public interface ProgressPainterFactory {
    public AbstractPainter<JComponent> createRegularBackgroundPainter();
    public AbstractPainter<JProgressBar> createRegularForegroundPainter();
}
