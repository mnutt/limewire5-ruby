package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.CellRendererPane;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

public final class ProgTabUIFactory {

    private ProgTabUIFactory() {}
    
    /**
     * The classes for each specific UI we know how to handle.
     */

    private static final Class aquaUI;
    private static final Class windowsUI;
    private static final Class metalUI;
    private static final Class basicUI;

    static {
        aquaUI = loadClass("apple.laf.AquaTabbedPaneUI");
        windowsUI = 
            loadClass("com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI");
        metalUI = javax.swing.plaf.metal.MetalTabbedPaneUI.class;
        basicUI = javax.swing.plaf.basic.BasicTabbedPaneUI.class;
    }

    /**
     * Load the specified class -- if it couldn't load, return null.
     */
    private static final Class loadClass(String toLoad) {
        try {
            return Class.forName(toLoad);
        } catch(ClassNotFoundException cfnfe) {
            return null;
        }
    }
    
    /**
     * Creates a new instance of the class if it could load.
     * If it couldn't be loaded, return the default UI.
     */
    private static TabbedPaneUI createNewUI(String toLoad,
                                            TabbedPaneUI def) {
        Class clazz = loadClass(toLoad);
        if( clazz == null )
            return def;
        
        try {
            return (TabbedPaneUI)clazz.newInstance();
        } catch(IllegalAccessException iae) {
            return def;
        } catch(InstantiationException iae) {
            return def;
        } catch(ExceptionInInitializerError eiie) {
            return def;
        } catch(SecurityException se) {
            return def;
        } catch(ClassCastException cce) {
            return def;
        }
    }

    /**
     * Update the UI of this JTabbedPane to contain a progress bar
     */
    public static void extendUI(JTabbedPane pane) {
        TabbedPaneUI oldUI = pane.getUI();
        TabbedPaneUI newUI = oldUI;

        if( aquaUI != null && aquaUI.isInstance(oldUI) )
            newUI = createNewUI("com.limegroup.gnutella.gui.AquaTab", newUI);
        else if( windowsUI != null && windowsUI.isInstance(oldUI) )
            newUI = createNewUI("com.limegroup.gnutella.gui.WinTab", newUI);
        else if( metalUI != null && metalUI.isInstance(oldUI) )
            newUI = new MetalTab();
        // Note that this last check is an == check.
        // This is because lots of UIs are going to extend from BasicUI
        // and we only want to use the BasicTab if we are positive that
        // the actual tab is using a BasicUI.
        else if( oldUI.getClass() == basicUI ) 
            newUI = new BasicTab();
        
        pane.setUI(newUI);
    }
    
    /**
     * For use with Windows L&F's on Java 1.3 and below.
     * Renders a shade darker than the background over the
     * entire tab, before the text is drawn.
     */
    private static class BasicTab extends BasicTabbedPaneUI {
        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement,
                                          int tabIndex, 
                                          int x, int y, int w, int h,
                                          boolean isSelected) {
            super.paintTabBackground(g, tabPlacement, tabIndex,
                                     x, y, w, h, isSelected);
            
            Color darker = tabPane.getBackgroundAt(tabIndex).darker();
            g.setColor(darker);
            
            long currentTime = System.currentTimeMillis();
            Progressor p = (Progressor)tabPane.getComponentAt(tabIndex);
            double percent = p.calculatePercentage(currentTime);
            if( percent > 1 )
                percent = 1.0;
            g.fillRect(x+1, y+1, (int)((w-3)*percent), h-1);
        }
    }

    /**
     * For use with the Metal L&F.
     * Draws a ProgressBar, starting with the left of the text area
     * over to the right-side of the whole tab.  Draws the text
     * ontop of the progressbar.
     */
    private static class MetalTab extends MetalTabbedPaneUI {
        private final JProgressBar PROGRESS = new LimeJProgressBar();
        private final CellRendererPane PANE = new CellRendererPane();
        
        public MetalTab() {
            super();
            PROGRESS.setMinimum(0);
            PROGRESS.setMaximum(100);
            PROGRESS.setBorderPainted(false);
            PROGRESS.setOpaque(false);
        }
    
        @Override
        protected void paintText(Graphics g, int tabPlacement,
                                Font font, FontMetrics metrics,
                                int tabIndex, String title,
                                Rectangle textRect, boolean isSelected) {
            long currentTime = System.currentTimeMillis();
            Progressor p = (Progressor)tabPane.getComponentAt(tabIndex);
            double percent = p.calculatePercentage(currentTime);
            if( percent > 1 )
                percent = 1.0;
        
            PROGRESS.setValue((int)(percent * 100));
        
            Rectangle tabRect = tabPane.getBoundsAt(tabIndex);
            int x, y, w, h;
            x = textRect.x - 4;
            y = textRect.y;
            w = tabRect.width + tabRect.x - x - 1;
            h = tabRect.height + tabRect.y - y;
            PANE.paintComponent(g, PROGRESS, tabPane.getParent(),
                                x, y, w, h);

            super.paintText(g, tabPlacement, font, metrics, tabIndex,
                            title, textRect, isSelected);
        }
    }
    
    public interface Progressor {
        public double calculatePercentage(long currentTime);
    }
}
