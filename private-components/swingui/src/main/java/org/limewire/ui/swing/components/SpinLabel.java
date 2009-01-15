package org.limewire.ui.swing.components;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * A label that will spin it's contents clockwise
 */
public class SpinLabel extends JXLabel {

    private static int FPS = 20;
    private static double FULL_ROTATION = 2*Math.PI;
    private static double TICK = FULL_ROTATION / 16;
    
    private final Timer repaintTimer;
    
    public SpinLabel() {

        this.setForegroundPainter(new SpinPainter());
        
        repaintTimer = new Timer(1000/FPS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
    }
    
    /**
     * Starts and stops the spin motion.  
     * 
     * Signature copied from JXBusyLabel
     */
    public void setBusy(boolean busy) {
        if (busy) {
            repaintTimer.start();
        } 
        else {
            repaintTimer.stop();
        }
    }
    
    private static class SpinPainter extends AbstractPainter<JXLabel> {

        private double theta = 0;
        
        public SpinPainter() {
            this.setCacheable(false);
            this.setAntialiasing(true);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXLabel object, int width, int height) {
            
            g.translate(width/2, height/2);
            g.rotate(theta);
            g.translate(-width/2, -height/2);
            object.paint(g); // Will not recurse back since JXLabel.painting == true
            
            theta = (theta + TICK)  %  FULL_ROTATION;
        }
    }
}
