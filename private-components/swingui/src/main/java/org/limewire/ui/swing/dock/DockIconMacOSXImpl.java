package org.limewire.ui.swing.dock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdesktop.swingx.geom.Star2D;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;

/**
 * A Mac OS X Dock Icon for LimeWire. Overlays the download
 * completion count on the LimeWire icon in the dock.
 */
class DockIconMacOSXImpl implements DockIcon {
    
    /**
     * The max number we can display (depends on the FONT!).
     */
    private static final int MAX_NUMBER = 999999;
    
    /**
     * The minimum width and height of the badge.
     */
    private static final int MIN_WnH = 42;
    
    /**
     * The Font that is used to draw the numbers.
     */
    private final Font FONT = new Font("Helvetica Neue", Font.BOLD, 26);
    
    /**
     * The number of complete Downloads.
     */
    private int complete = 0;
    
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(new Service() {
            public void start() {}

            @Asynchronous (daemon = false)
            public void stop() {
                Dock.restoreDockTileImage();
            }

            public void initialize() {}

            public String getServiceName() {
                return "Dock cleanup";
            }
        }).in("UIHack");
    }
    
    /*
     * (non-Javadoc)
     * @see javax.swing.Icon#getIconWidth()
     */
    public int getIconWidth() {
        return Dock.ICON_WIDTH;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.swing.Icon#getIconHeight()
     */
    public int getIconHeight() {
        return Dock.ICON_HEIGHT;
    }
    
    /**
     * Draws the LimeWire Dock Icon. Only draws if the count
     * has changed since the last draw.
     * 
     * @param complete The number of complete Downloads
     */
    public void draw(int complete) {
        synchronized (Dock.getDockLock()) {
            if (complete != this.complete) {
                this.complete = complete;
                
                Dock.restoreDockTileImage();
                
                if (complete > 0)
                    Dock.setDockTileOverlayImage(this);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        String value = Integer.toString(Math.min(complete, MAX_NUMBER));

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setFont(FONT);
        FontMetrics fm = g2.getFontMetrics();
        int valueWidth = fm.stringWidth(value);
        
        int width = MIN_WnH;
        if (value.length() > 2) {
            width = Math.max(valueWidth + 16, width);
        }
        
        int outerRadius = width / 2;
        int innerRadius = outerRadius - 4;
        Star2D star = new Star2D(x+Dock.ICON_WIDTH-width-5, y, innerRadius, outerRadius, 25); 
        
        g2.setStroke(new BasicStroke(1.5f));
        g2.setPaint(Color.RED);
        g2.fill(star);
        
        g2.setPaint(Color.white);
        g2.drawString(value, (int)(star.getX() + (star.getOuterRadius() * 2 - valueWidth) / 2.1), (int)(star.getY() + (fm.getHeight() * 0.93f)));
    }
    
}