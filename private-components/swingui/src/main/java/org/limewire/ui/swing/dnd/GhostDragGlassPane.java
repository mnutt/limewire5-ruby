package org.limewire.ui.swing.dnd;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Panel which gets installed as the glass pane of a Window. 
 * This glass pane creates and displays semi-transparent images
 * over the main application.
 * 
 * It is the responsibility of drag listeners to update the positioning,
 * image and visibility of this glass pane.
 */
@Singleton
public class GhostDragGlassPane extends JPanel {

    private DragPanel dragPanel;
    private float alpha = 0.85f;
    
    private BufferedImage dragged = null;
    private Point location = new Point(0, 0);
    private Point oldLocation = new Point(0, 0);
    
    private int width;
    private int height;
    private Rectangle visibleRect = null;
    
    @Inject
    public GhostDragGlassPane() {
        setOpaque(false);
        
        dragPanel = new DragPanel();
        dragPanel.setSize(new Dimension(200,60));
    }
    
    /**
     * Updates the image properlly based on the type of Friend.
	 * If friend is null, a default adding to My Library message
     * is displayed, otherwise the friend's name is included in
     * the shared message.
     */
    public void setText(Friend friend) {
        dragPanel.setText(friend);
        dragPanel.revalidate();

        Dimension size = dragPanel.getSize();
        BufferedImage myImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = myImage.createGraphics();
        dragPanel.paint(g2);
        g2.dispose();
        setImage(myImage);
    }
    
    private void setImage(BufferedImage image) {
        if (dragged != null) {
            float ratio = (float) dragged.getWidth() / (float) dragged.getHeight();
            this.width = image.getWidth();
            height = (int) (width / ratio);
        }

        this.dragged = image;
    }
    
    /**
     * Relocates the image to this point. The image is displayed
     * to the right of this location and 50% above/below the 
     * y coordinate of this location.
     * @param location
     */
    public void setPoint(Point location) {
        this.oldLocation = this.location;
        this.location = location;
    }
    
    /**
     * Returns the rectangle of where changes have occured
     * on the glass pane. This can greatly improve performance
     * by not repainting the entire glass pane.
     */
    public Rectangle getRepaintRect() {
        int x = (int) (location.getX());
        int y = (int) (location.getY() - (height/ 2));
        
        int x2 = (int) (oldLocation.getX());
        int y2 = (int) (oldLocation.getY() - (height/ 2));
        
        return new Rectangle(x, y, width, height).union(new Rectangle(x2, y2, width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (dragged == null || !isVisible()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int x = (int) (location.getX());
        int y = (int) (location.getY() - (height/ 2));
        
        if (visibleRect != null) {
            g2.setClip(visibleRect);
        }
        
        g2.drawImage(dragged, x, y, width, height, null);
        g2.dispose();
    }
    
    /**
     * Actual panel that gets painted on the glass pane. This
     * panel is never displayed directly on the glass pane.
     * Its used to update text, then its graphics are painted 
     * to a BufferedImage. This BufferedImage is in turn
     * painted on the glass pane. This is much more efficient
     * way of displaying this component while it is being 
     * moved.
     */
    private class DragPanel extends JPanel {
        private JLabel label;
        
        public DragPanel() {
            setBorder(BorderFactory.createMatteBorder(2,2,2,2, Color.BLACK));

            setBackground(Color.WHITE);
            
            setLayout(new BorderLayout());
            label = new JLabel("");
            label.setSize(new Dimension(200,40));
            label.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            
            add(label, BorderLayout.CENTER);
        }
        
        public void setText(Friend friend) {
            if(friend == null) {
            	label.setText(I18n.tr("Add to My Library"));
            } else {
                String renderName = friend.getRenderName();
                if(!StringUtils.isEmpty(renderName)) {
                    label.setText(I18n.tr("Share files with {0}", renderName));
                } else {
                    label.setText(I18n.tr("Share files"));
                }
            }
        }
    }
}
