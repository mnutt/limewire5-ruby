package com.limegroup.gnutella.gui.mp3;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.UIManager;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/**
 * A Panel that draws rounded corners with an inset border. Currently uses the table
 * header color to draw the background. The inset is achieved by using lighter and darker 
 * colors derived from the background color. 
 */
public class MediaPlayerPanel extends BoxPanel implements ThemeObserver, ComponentListener{

    public MediaPlayerPanel(int orientation){
        super(orientation);
        
        // since we are painting rounded corners, set to false to ensure
        // the parent will always repaint to hide the corners
        this.setOpaque(false);
        
        ThemeMediator.addThemeObserver(this);
        
        this.addComponentListener(this);
        
        updateTheme();
    }
    
    @Override
    protected void paintComponent(Graphics g){
        Rectangle dimension = this.getVisibleRect();

        Graphics2D g2 = (Graphics2D)g;
        Object oldAntialiase = 
            g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        // make the rendering look nice with antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor( getBackground());
        g2.fillRoundRect(dimension.x, dimension.y, dimension.width, 
                dimension.height, 18, 18);
        
        g2.setColor(getBackground().darker().darker());
        g2.drawRoundRect(dimension.x, dimension.y, dimension.width-1, 
               3* dimension.height/4, 18, 18);
        g2.setColor(getBackground().darker());
        g2.drawRoundRect(dimension.x, dimension.y+1, dimension.width-1, 
                3* dimension.height/4, 18, 18);

        g2.setColor(getBackground().brighter().brighter());
        g2.drawRoundRect(dimension.x , dimension.y + dimension.height/4-0, dimension.width-1, 
                3* dimension.height/4, 18, 18);
        g2.setColor(getBackground().brighter());
        g2.drawRoundRect(dimension.x , dimension.y + dimension.height/4-1, dimension.width-1, 
                3* dimension.height/4, 18, 18);
        g2.setColor( getBackground() );
        g2.fillRoundRect(dimension.x+1, dimension.y+2, dimension.width-2, 
                dimension.height-4, 18, 18);
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiase);
    }

    public void updateTheme() {
        this.setBackground(UIManager.getColor("TableHeader.background"));
        repaint();
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        repaint();
    }

    public void componentShown(ComponentEvent e) {
    }
}
