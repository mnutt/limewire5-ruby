package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * Draws a circle, with a given width in the center of the component.
 */
public class Circle extends JComponent {

    private int diameter;
    
    public Circle() {
        this(1);
    }
    
    public Circle(int diameter) {
        this.diameter = diameter;
        this.setPreferredSize(new Dimension(diameter, diameter));
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponents(g);
        
        Graphics2D g2 = (Graphics2D) g;

        Color oldColor = g2.getColor();
        
        // get original antialiasing value for reset
        Object origAntiAliasHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getForeground());
        
        int x;
        int y;
        if(diameter > getWidth())
            x = 0;
        else
            x = (getWidth() - diameter) /2;
        if(diameter > getHeight())
            y = 0;
        else
            y = (getHeight() - diameter)/2;
        g2.fillOval(x, y, diameter, diameter);
        
        g2.setColor(oldColor);

        // reset antialiasing propery
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint);
    }    
}
