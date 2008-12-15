package com.limegroup.gnutella.gui.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * Draws a circle with shaded sides.
 */
public final class CircularIcon implements Icon {
    private static final Icon INSTANCE = new CircularIcon();
    private CircularIcon() {}
    public static Icon instance() { return INSTANCE; }
    
  protected int width =  10;
  protected int height = 10;
  
  public int getIconWidth() {
    return width;
  }
  
  public int getIconHeight() {
    return height;
  }
  
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Color bg = c.getBackground();
    // Compute two good contrasting shades of the background colors
    Color light = null;
    Color shade = null;
    if (bg.getRed() >= 0xFC && bg.getGreen() >= 0xFC && bg.getBlue() >= 0xFC) {
      light = bg.darker();
      shade = light.darker();
    } else
    if (bg.getRed() <= 0x03 && bg.getGreen() <= 0x03 && bg.getBlue() <= 0x03) {
      shade = bg.brighter();
      light = shade.brighter();
    } else {
      light = bg.brighter();
      shade = bg.darker();
    }
  
    int w = width;
    int h = height;

      g.setColor(shade);
      g.drawArc(x, y, w-2, h-2, -45, 180);
      g.setColor(light);
      g.drawArc(x, y, w-2, h-2, -45+180, 180);
  }
}

