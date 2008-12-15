package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

import javax.swing.UIManager;

import sun.java2d.SunGraphics2D;
import apple.laf.AquaImageFactory;
import apple.laf.AquaTabbedPaneUI;

/**
 * A progress-bar tab for use with OSX on Java 1.4.
 */
public class AquaTab extends AquaTabbedPaneUI {
    
    private static final int TRANSLUCENCY = 0x80000000;
    private static final int LIME_GREEN = 0x00179524; // ARGB
    
    /*
     * TODO: Check on every new Mac OS X release if Apple 
     * has changed the shape of the Tabs and if adjustemns
     * are necessary (and if vertical offsets are necessary).
     * It should look OK without the offsets though! This is
     * just finetuning!
     * 
     * Current: OSX 10.3 and 10.4
     */
    private static final int TOP_OFFSET = 3;
    private static final int BOTTOM_OFFSET = 1;
    
    public AquaTab() {
    }
    
    /**
     * Extended to paint the progress bar.
     */
    protected void paintContents(Graphics g, int tabPlacement, int selTab,
            Rectangle tabRect, Rectangle iconRect, Rectangle tabRect2,
            boolean isSelected) {
        
        // The size check is necessary because an extra tab
        // is created that lets the user choose which hidden
        // tabs should be displayed.  This tab has no component
        // and is not really part of the JTabbedPane, and thus
        // causes an IndexOutOfBoundsException when getting the
        // component.  The extra tab is only created when there
        // are too many tabs to be displayed in a single tab run.
        if(selTab < tabPane.getTabCount()) {

            // The basic idea is to draw an offscreen image of the Tab 
            // and to use its alpha mask to draw the pseudo progress
            // indicator which will have the same shape as the actual
            // Tab. Here and there a bit translucency and it looks
            // like as if the progress indicator is part of the Tab
            // but it's actually drawn on the top of it
            final int w = tabRect.width;
            final int h = tabRect.height;
            
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            
            // Should be always the case but who knows?
            if (g2 instanceof SunGraphics2D) {
                //final int VERTICAL = 0;
                final int HONIZONTAL = 1;
                
                boolean selected = false;//isSelected;
                boolean hidden = false;
                boolean active = true;
                boolean leftCap = (selTab == 0);
                boolean rightCap = (selTab == tabPane.getTabCount()-1);
                boolean focused = false;
                
                // Draw the Tab
                AquaImageFactory.drawTab((SunGraphics2D)g2, 0, 0, w, h, 
                        HONIZONTAL, selected, hidden, false, active, leftCap, rightCap, focused, false);
            }
            g2.dispose();
            img.flush();
            
            DataBuffer src = img.getAlphaRaster().getDataBuffer();
            
            long currentTime = System.currentTimeMillis();
            ProgTabUIFactory.Progressor prog =
                (ProgTabUIFactory.Progressor)tabPane.getComponentAt(selTab);
            double percent = prog.calculatePercentage(currentTime);
            if(percent > 1)
                percent = 1.0;
            
            BufferedImage progress = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            DataBuffer dst = progress.getAlphaRaster().getDataBuffer();
            
            final int p = (int)(w * percent);
            for(int y = TOP_OFFSET; y < (h-BOTTOM_OFFSET); y++) {
                for(int x = 0; x < p; x++) {
                    int index = (y*w)+x;
                    
                    int value = src.getElem(index);
                    dst.setElem(index, (value & TRANSLUCENCY) | getColor());
                }
            }
            progress.flush();
            
            g.drawImage(progress, tabRect.x, tabRect.y, null);
        }
        
        super.paintContents(g, tabPlacement, selTab, tabRect, iconRect, tabRect2, isSelected);
    }
    
    private static int getColor() {
        Color selectionBackground 
            = UIManager.getDefaults().getColor("TextField.selectionBackground");
        
        if (selectionBackground == null)
            return LIME_GREEN;
        
        // Make it a bit darker as cobinations like Aqua
        // theme (Blue) and Blue highlight color are barely
        // visible. Same for Graphite and Graphite or Silver!
        selectionBackground = selectionBackground.darker();
        return (selectionBackground.getRed() << 16) 
                | (selectionBackground.getGreen() << 8) 
                | (selectionBackground.getBlue());
    }
}
