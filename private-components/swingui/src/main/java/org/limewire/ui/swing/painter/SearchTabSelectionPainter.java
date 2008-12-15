package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.util.GuiUtils;

public class SearchTabSelectionPainter extends AbstractPainter<FancyTab> {
    
    @Resource private Color topBevelBackground;
    @Resource private Color topBevelBorder;
    @Resource private Color backgroundTopGradient;
    @Resource private Color backgroundBottomGradient;
    
    private final GradientPaint gradient;
    
    private Area tabAreaCache   = null;
    private int  tabWidthCache  = 0;
    private int  tabHeightCache = 0;
    
    public SearchTabSelectionPainter() {
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(true);
        this.setCacheable(true);
        
        gradient = new GradientPaint(0, 0, this.backgroundTopGradient, 0, 1, this.backgroundBottomGradient);
    }

    
    private void cacheTabArea(int width, int height) {
        if (this.tabWidthCache == width && this.tabHeightCache == height)  return;        
        
        // System.out.println("Hard Miss: " + this);
        
        Area compound = new Area(new RoundRectangle2D.Float(10, 3, width-1-20, height-1, 20, 20));
        
        compound.add(new Area(new Rectangle2D.Float(0,height-10-1,width-1,height-1)));
        
        compound.subtract(new Area(new Arc2D.Float(-10,height-1-21,20,20,270,90,Arc2D.PIE)));
        compound.subtract(new Area(new Arc2D.Float(width-1-10,height-1-21,20,20,360,270,Arc2D.PIE)));
        
        this.tabAreaCache   = compound;
        this.tabWidthCache  = width;
        this.tabHeightCache = height;
    }
    
    @Override
    protected void doPaint(Graphics2D g, FancyTab object, int width, int height) {
        // System.out.println("Soft Miss: " + this);
        
        cacheTabArea(width, height);

        // Draw top bevel
        g.setColor(this.topBevelBackground);
        g.fillRoundRect(10, 0, width-2-20, 20, 20, 20);
        
        // Draw top border
        g.setColor(this.topBevelBorder);
        g.drawRoundRect(10, 0, width-2-20, 20, 20, 20);
        
        // Draw tab
        g.setPaint(gradient);
        g.fill(this.tabAreaCache);
        
        // Hack for Anthony to correct the antialiasing for the bottom border and tab mixing
        g.setColor(new Color(0xca,0xca,0xca));
        g.drawLine(0,height-2,0,height-2);
        g.drawLine(width-1,height-2,width-1,height-2);
        g.setColor(new Color(0xab,0xab,0xab));
        g.drawLine(1,height-2,1,height-2);
        g.drawLine(width-2,height-2,width-2,height-2);
    }

}