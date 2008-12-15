package com.limegroup.gnutella.gui.xml.editor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.library.FileDesc;

public class IconPanel extends JPanel {
    
    private BufferedImage cachedImage;

    private Icon icon;
    
    public IconPanel() {
    }
    
    public void initWithFileDesc(FileDesc fd) {
        IconManager iconManager = IconManager.instance();
        icon = iconManager.getIconForFile(fd.getFile());
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (icon != null) {
            if (cachedImage == null || cachedImage.getWidth() != getWidth()
                    || cachedImage.getHeight() != getHeight()) {
                createCachedImage();
        }
            g.drawImage(cachedImage, 0, 0, null);
        }
    }
    
    private void createCachedImage(){
        GraphicsConfiguration gc = 
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        cachedImage = gc.createCompatibleImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = (Graphics2D) cachedImage.getGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(((ImageIcon) icon).getImage(), 0, 0, cachedImage.getWidth(),
        cachedImage.getHeight(), null);
        g2.dispose();
    }
}
