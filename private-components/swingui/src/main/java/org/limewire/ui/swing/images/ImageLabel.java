package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.limewire.ui.swing.util.GuiUtils;

/**
 * Draws an image, centered in the component. 
 */
public class ImageLabel extends JComponent {

    protected int topPadding = 5;
    
    private Icon icon;
    
    private Border border;
    
    private JComponent buttonComponent;
    
    private JLabel label;
    
    private int x = 0;
    private int y = 0;
    
    public ImageLabel(int width, int height) {
        GuiUtils.assignResources(this);
        
        setOpaque(true);
        
        setPreferredSize(new Dimension(width, height));
        setSize(getPreferredSize());
        setLayout(null);
        
        label = new JLabel();
        label.setVisible(false);
        label.setOpaque(false);
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        calculateLabelDimnensions();
        add(label);
    }
    
    public void setIcon(Icon icon) {
        this.icon = icon;
    }
    
    public void setInsets(Border border) {
        super.setBorder(border);
        if(buttonComponent != null)
            calculateSubPanelDimensions();
    }
    
    @Override
    public void setBorder(Border border) {
        this.border = border;
    }
    
    /**
     * Sets the subComponent on this. This gets painted below the image.
     */
    public void setButtonComponent(JComponent buttonPanel) {
        this.removeAll();
        
        this.buttonComponent = buttonPanel;        
        calculateSubPanelDimensions();
        add(buttonPanel);
        add(label);
    }
    
    public void setText(String text) {
        label.setText(text);
        if(text == null || text.length() == 0)
            label.setVisible(false);
        else
            label.setVisible(true);
    }
    
    /**
     * Returns the Point where the subComponent is located.
     */
    public Point getSubComponentLocation() {
        return new Point(x,y);
    }
    
    private void calculateSubPanelDimensions() {
        x = (getWidth() - getInsets().left - getInsets().right - ThumbnailManager.WIDTH)/2 + getInsets().left;
        y = getInsets().top + ThumbnailManager.HEIGHT +  topPadding;
        int width = ThumbnailManager.WIDTH;
        int height =  getHeight() - ThumbnailManager.HEIGHT - topPadding - getInsets().top - getInsets().bottom;

        buttonComponent.setBounds(x, y, width, height);
    }
    
    private void calculateLabelDimnensions() {
        int x = (getWidth() - getInsets().left - getInsets().right - ThumbnailManager.WIDTH)/2 + getInsets().left;
        int y = getInsets().top + ThumbnailManager.HEIGHT +  topPadding;
        label.setBounds(x, y - 10, ThumbnailManager.WIDTH, 26);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if(isVisible()) {          
            int x = getInsets().left;
            int y = getInsets().top;
            int width = getWidth() - getInsets().left - getInsets().right;
            int height = getHeight() - getInsets().top - getInsets().bottom;
            
            g.setColor(getBackground());
            g.fillRect(getInsets().left, getInsets().top, getWidth() - getInsets().left - getInsets().right,
                    getHeight() - getInsets().top - getInsets().bottom);
            
            // if a border has been added, ask it to paint itself
            if(border != null) {
                border.paintBorder(this, g, x, y, width, height);
            }
            // if an icon is loaded
            if(icon != null) {
                int iconX = (width - icon.getIconWidth())/2;
                int iconY = (ThumbnailManager.HEIGHT - icon.getIconHeight())/2;
             
                icon.paintIcon(this, g, x + iconX, topPadding + y + iconY);
            }
        }
    }
}