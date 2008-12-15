package org.limewire.ui.swing.options;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.options.actions.TabAction;

/**
 * Paints a main button on the Option Panel
 */
public class FancyOptionTabButton extends JXButton {
    
    public FancyOptionTabButton(TabAction action) {
        super(action);
        
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setIconTextGap(0);
        setPreferredSize(new Dimension(getPreferredSize().width, 50));
        setGradients(Color.gray, Color.gray.brighter().brighter());
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorder(null);
        setOpaque(false);
    }
    
    public void setGradients(Color topGradient, Color bottomGradient) {
        getAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    repaint();
                }
            }
        });
        
        final Painter<JXButton> oldPainter = getBackgroundPainter();
        setBackgroundPainter(new MattePainter<JXButton>(
                new GradientPaint(new Point2D.Double(0, 0), topGradient, 
                        new Point2D.Double(0, 1), bottomGradient,
                        false), true) {
            @Override
            public void doPaint(Graphics2D g, JXButton component, int width,
                    int height) {
                if(Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY))) {
                    super.doPaint(g, component, width, height);
                } else {
                    oldPainter.paint(g, component, width, height);
                }
            }
        });
    }
}
