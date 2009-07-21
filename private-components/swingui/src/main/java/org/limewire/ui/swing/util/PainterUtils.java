package org.limewire.ui.swing.util;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class PainterUtils {
   
    
    public static final Color TRASPARENT = new Color(0,0,0,0);
    public static final Insets BLANK_INSETS = new Insets(0,0,0,0);
    
    /**
     * Draws a string to a graphics with anti-aliasing
     */
    public static void drawSmoothString(Graphics g, String s, int x, int y) {
        
        Graphics2D g2 = (Graphics2D) g;
        
        // Get original anti-aliasing value for reset
        Object origAntiAliasHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        // Turn on anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        
        // Draw the string
        g2.drawString(s, x, y);

        // Reset anti-aliasing property
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint);
    }

    /**
     * Produces a new lightened colour value by a given intensity from a base colour.
     */
    public static Color lighten(Color orig, int intensity) {
        try {
            return new Color(orig.getRed() + intensity,
                orig.getGreen() + intensity,
                orig.getBlue() + intensity);
        } catch (IllegalArgumentException e) {
            if (intensity > 0) {
                // Return transparent if the colour is brightened by too much
                return new Color(0,0,0,0);
            } else {
                // Return black if colour is darkened by too much
                return Color.BLACK;
            }
        }
    }

    /** 
     * Creates a new softening filter based on a softening factor.
     */
    public static ConvolveOp createSoftenFilter(float factor) {
 
        float[] transform = {0     , factor    , 0     , 
                             factor, 1-factor*4, factor, 
                             0     , factor    , 0    };
        
        Kernel kernel = new Kernel(3, 3, transform);
        
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        
    }
    
    public static Color appendAlpha(Color c, double opacity) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(opacity*255));
    }
    
    /**
     * Simplifies a paint to a colour.  If the paint is already a colour
     * then simply cast.  If it is a gradient then return the second point.
     * If unknown then return transparent. 
     * <p>
     * Note: In future may consider returning null, however right now
     *        transparent allows us to leverage null colours possibly
     *        from empty props.
     */
    public static Color getColour(Paint paint) {
        if (paint instanceof Color) {
            return (Color) paint;
        }
        
        if (paint instanceof GradientPaint) {
            return ((GradientPaint) paint).getColor2();
        }
        
        return null;
    }
}
