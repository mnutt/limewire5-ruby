package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.painter.BusyPainter;
import org.jdesktop.swingx.plaf.UIManagerExt;

public class ColoredBusyLabel extends JXBusyLabel {
    
    private final Color a;
    private final Color b;

    public ColoredBusyLabel(Dimension dimension, Color a, Color b) {
        super(dimension);
        this.a = a;
        this.b = b;
        setBusyPainter(null); // null out the painter the super constructor set.
        getBusyPainter(); // recreate.
    }

    @Override
    protected BusyPainter createBusyPainter(final Dimension dim) {
        BusyPainter p = new BusyPainter() {
            @Override
            protected void init(Shape point, Shape trajectory, Color unused, Color unused2) {
                super.init(dim == null ? 
                                UIManagerExt.getShape("JXBusyLabel.pointShape") : getScaledDefaultPoint(dim.height),
                           dim == null ?
                                UIManagerExt.getShape("JXBusyLabel.trajectoryShape") : getScaledDefaultTrajectory(dim.height),
                           a, b);
            }
        };
        return p;
    }

}
