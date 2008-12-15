package com.limegroup.gnutella.gui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.CellRendererPane;
import javax.swing.JProgressBar;

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI;

/**
 * The progress-bar tab to use on Windows.
 */
public class WinTab extends WindowsTabbedPaneUI {
    
    private final JProgressBar PROGRESS = new LimeJProgressBar();
    private final CellRendererPane PANE = new CellRendererPane();
    
    public WinTab() {
        super();
        PROGRESS.setMinimum(0);
        PROGRESS.setMaximum(100);
        PROGRESS.setBorderPainted(true);
        PROGRESS.setStringPainted(true);
    }
    
    /**
     * Extended to paint the progress bar.
     */
    protected void paintText(Graphics g, int tabPlacement,
                            Font font, FontMetrics metrics,
                            int tabIndex, String title,
                            Rectangle textRect, boolean isSelected) {
        long currentTime = System.currentTimeMillis();
        ProgTabUIFactory.Progressor p =
            (ProgTabUIFactory.Progressor)tabPane.getComponentAt(tabIndex);
        double percent = p.calculatePercentage(currentTime);
        if( percent > 1 )
            percent = 1.0;
        
        PROGRESS.setValue((int)(percent * 100));
        PROGRESS.setString(title);
        PROGRESS.setFont(font);
        
        Rectangle tabRect = tabPane.getBoundsAt(tabIndex);
        int x, y, w, h;
        x = textRect.x - 4;
        y = textRect.y;
        w = tabRect.width + tabRect.x - x - 2;
        h = tabRect.height + tabRect.y - y;
        PANE.paintComponent(g, PROGRESS, tabPane.getParent(), x, y, w, h);
    }
}