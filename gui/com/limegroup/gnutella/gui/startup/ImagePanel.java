package com.limegroup.gnutella.gui.startup;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.limegroup.gnutella.gui.MultiLineLabel;

/**
 * Draws an image as the background of the component. Also
 * adds a multi-row label overlay on the right side of component
 */
public class ImagePanel extends JComponent implements ComponentListener {

    /**
     * Image to paint as the background
     */
    private Image backgroundImage;
    
    /**
     * Text to display over the background image
     */
    private String text;
    
    /**
     * Maximum width of the text label
     */
    private int labelWidth;
    
    private MultiLineLabel label;
    
    /**
     * Default label size
     */
    private final Dimension labelSize = new Dimension(250,100);
    
    /**
     * Default start position for the label
     */
    private final Point labelPosition = new Point(100,8);
    
    /**
     * percentage of where the label starts compared to the component width
     * this is needed when the component is resized to make sure the text 
     * will be pushed right as the image grows
     */
    private final float pos = .275f;
    
    public ImagePanel(){
        this(null);
    }
    
    public ImagePanel(String text, int labelWidth) {
        this(null, text, labelWidth);
    }
    
    public ImagePanel(ImageIcon image) {
        this(image,"", 0);
    }
    
    public ImagePanel(ImageIcon image, String text, int labelWidth) {
        this.backgroundImage = (image == null) ? null : image.getImage();
        this.text = text;
        this.labelWidth = labelWidth;

        this.setLayout(null);
        initLabel();
        
        this.addComponentListener(this);
    }
    
    /**
     * Creates the label to overlay text ontop of the image
     */
    private void initLabel(){
        label = new MultiLineLabel(text, labelWidth, true);
        label.setFont(new Font("Dialog", Font.PLAIN, 12));
        label.setForeground(Color.BLACK);
        label.setLocation(labelPosition);
        label.setSize( labelSize );
        
        this.add(label);
    }
    
    @Override
    public void paintComponent(Graphics g) { 
        super.paintComponents(g);
        if( backgroundImage != null && 
                backgroundImage.getWidth(this) > 0 && 
                backgroundImage.getHeight(this) > 0) { 
            g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
        }
    }
    
    public void setBackgroundImage(Image image) {
        backgroundImage = image; 
        repaint();
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public void setLabelWidth(int width){
        this.labelWidth = width;
    }

    public void componentHidden(ComponentEvent e) {
    }
    public void componentMoved(ComponentEvent e) {
    }
    public void componentShown(ComponentEvent e) {
    }

    /**
     * When the component is resized, update the label size and position to
     * flow with the image smoothly
     */
    public void componentResized(ComponentEvent e) {
        int labelStartPos = (int) (this.getWidth() * pos);
        label.setLocation(labelStartPos, labelPosition.y);
        label.setSize( new Dimension(this.getWidth() - labelStartPos - 5, this.getHeight()));
    }


}
