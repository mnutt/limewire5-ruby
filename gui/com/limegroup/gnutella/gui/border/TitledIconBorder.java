package com.limegroup.gnutella.gui.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * An TitledBorder that also draws an icon.
 *
 * THIS CLASS IS ONLY TESTED TO LOOK DECENT WITH
 * A LEFT-TO-RIGHT LAYOUT AND THE TITLE ON THE TOP.
 */
public class TitledIconBorder extends TitledBorder {
    
    protected Icon icon;
    
    private Point textLoc = new Point();
    private Point iconLoc = new Point();
    
    /**
     * Space between the edge of the component & the border.
     */
    private static final int BORDER_SPACING = 2;
    
    /**
     * Space between icon & text.
     */
    private static final int ICON_SPACING = 4;
    
    // Horizontal inset of title that is left or right justified
    static protected final int TITLE_INSET_H = 8;
    
    /**
     * Space between the border & icon and the text & border.
     */
    private static final int EMPTY_SPACE = 4;
    
    public TitledIconBorder() {
        super(UIManager.getBorder("TitledBorder.border"));
    }

    public TitledIconBorder(Border border) {
        super(border);
    }
    
    public void setIcon(Icon icon) {
        this.icon = icon;
    }
    
    public Icon getIcon() {
        return icon;
    }
    
    /**
     * Paints the border for the specified component with the 
     * specified position and size.
     * @param c the component for which this border is being painted
     * @param g the paint graphics
     * @param x the x position of the painted border
     * @param y the y position of the painted border
     * @param width the width of the painted border
     * @param height the height of the painted border
     */
    @Override
    @SuppressWarnings("deprecation")
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if(icon == null) {
            super.paintBorder(c, g, x, y, width, height);
            return;
        }
        
        Border border = getBorder();
        final String title = getTitle();
        final int titlePos = getTitlePosition() == DEFAULT_POSITION ?
            TOP : getTitlePosition();

        if (title == null || title.equals("")) {
            if (border != null) {
                border.paintBorder(c, g, x, y, width, height);
            }
            return;
        }

        Rectangle grooveRect = new Rectangle(x + EDGE_SPACING, y + EDGE_SPACING,
                                             width - (EDGE_SPACING * 2),
                                             height - (EDGE_SPACING * 2));
        Font font = g.getFont();
        Color color = g.getColor();

        g.setFont(getFont(c));

        JComponent jc = (c instanceof JComponent) ? (JComponent)c : null;
        FontMetrics fm = jc == null ? 
            Toolkit.getDefaultToolkit().getFontMetrics(font) :
            jc.getFontMetrics(font);
        int         titleHeight = Math.max(icon.getIconHeight(), fm.getHeight());
        int         descent = fm.getDescent();
        int         ascent = fm.getAscent();
        int         diff;
        int         titleWidth = fm.stringWidth(title) + ICON_SPACING + icon.getIconWidth();
        Insets      insets;

        if (border != null) {
            insets = border.getBorderInsets(c);
        } else {
            insets = new Insets(0, 0, 0, 0);
        }

        switch (titlePos) {
            case ABOVE_TOP:
                diff = ascent + descent + (Math.max(EDGE_SPACING,
                                 BORDER_SPACING*2) - EDGE_SPACING);
                grooveRect.y += diff;
                grooveRect.height -= diff;
                textLoc.y = grooveRect.y - (descent + BORDER_SPACING);
                iconLoc.y = grooveRect.y + BORDER_SPACING;
                break;
            case TOP:
                diff = Math.max(0, ((ascent/2) + BORDER_SPACING) - EDGE_SPACING);
                grooveRect.y += diff;
                grooveRect.height -= diff;
                textLoc.y = (grooveRect.y - descent) +
                (insets.top + ascent + descent)/2;
                iconLoc.y = grooveRect.y - (insets.top / 2);
                break;
            case BELOW_TOP:
                textLoc.y = grooveRect.y + insets.top + ascent + BORDER_SPACING;
                iconLoc.y = grooveRect.y - insets.top - BORDER_SPACING;
                break;
            case ABOVE_BOTTOM:
                textLoc.y = (grooveRect.y + grooveRect.height) -
                (insets.bottom + descent + BORDER_SPACING);
                iconLoc.y = grooveRect.y + grooveRect.height + insets.bottom + BORDER_SPACING;
                break;
            case BOTTOM:
                grooveRect.height -= titleHeight/2;
                textLoc.y = ((grooveRect.y + grooveRect.height) - descent) +
                        ((ascent + descent) - insets.bottom)/2;
                iconLoc.y = grooveRect.y + grooveRect.height + (insets.bottom/2);
                break;
            case BELOW_BOTTOM:
                grooveRect.height -= titleHeight;
                textLoc.y = grooveRect.y + grooveRect.height + ascent +
                        BORDER_SPACING;
                iconLoc.y = grooveRect.y + grooveRect.height - BORDER_SPACING;
                break;
        }
/* // not java 1.1.8 compatable. :(
	int justification = getTitleJustification();
	if(c.getComponentOrientation().isLeftToRight()) {
	    if(justification==LEADING || 
	       justification==DEFAULT_JUSTIFICATION) {
	        justification = LEFT;
	    }
	    else if(justification==TRAILING) {
	        justification = RIGHT;
	    }
	}
	else {
	    if(justification==LEADING ||
	       justification==DEFAULT_JUSTIFICATION) {
	        justification = RIGHT;
	    }
	    else if(justification==TRAILING) {
	        justification = LEFT;
	    }
	}
*/
        int justification = LEFT;

        switch (justification) {
            case LEFT:
                iconLoc.x = grooveRect.x + TITLE_INSET_H + insets.left;
                textLoc.x = iconLoc.x + icon.getIconWidth() + ICON_SPACING;
                break;
            case RIGHT:
                iconLoc.x = grooveRect.x + grooveRect.width -
                        (titleWidth + TITLE_INSET_H + insets.right);
                textLoc.x = iconLoc.x - icon.getIconWidth() - ICON_SPACING;
                break;
            case CENTER:
                iconLoc.x = grooveRect.x +
                        ((grooveRect.width - titleWidth) / 2);
                textLoc.x = iconLoc.x + icon.getIconWidth() + ICON_SPACING;
                break;
        }
        
        // If title is positioned in middle of border AND its fontsize
	// is greater than the border's thickness, we'll need to paint 
	// the border in sections to leave space for the component's background 
	// to show through the title.
        //
        if (border != null) {
            if (((titlePos == TOP) &&
		  (grooveRect.y > textLoc.y - ascent)) ||
		 (titlePos == BOTTOM && 
		  (grooveRect.y + grooveRect.height < textLoc.y + descent))) {
		  
                Rectangle clipRect = new Rectangle();
                
                // save original clip
                Rectangle saveClip = g.getClipBounds();

                // paint strip left of text
                clipRect.setBounds(saveClip);
                if (computeIntersection(clipRect, x, y, iconLoc.x-EMPTY_SPACE-x, height)) {
                    g.setClip(clipRect);
                    border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                  grooveRect.width, grooveRect.height);
                }

                // paint strip right of text
                clipRect.setBounds(saveClip);
                if (computeIntersection(clipRect, iconLoc.x+titleWidth+EMPTY_SPACE+2, y,
                               x+width-(iconLoc.x+titleWidth+EMPTY_SPACE+2), height)) {
                    g.setClip(clipRect);
                    border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                  grooveRect.width, grooveRect.height);
                }

                if (titlePos == TOP) {
                    // paint strip below text
                    clipRect.setBounds(saveClip);
                    if (computeIntersection(clipRect, iconLoc.x-EMPTY_SPACE, textLoc.y+descent, 
                                        titleWidth+(EMPTY_SPACE*2)+2, y+height-textLoc.y-descent)) {
                        g.setClip(clipRect);
                        border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                  grooveRect.width, grooveRect.height);
                    }

                } else { // titlePos == BOTTOM
		  // paint strip above text
                    clipRect.setBounds(saveClip);
                    if (computeIntersection(clipRect, iconLoc.x-EMPTY_SPACE, y, 
                          titleWidth+(EMPTY_SPACE*2)+2, textLoc.y - ascent - y)) {
                        g.setClip(clipRect); 
                        border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                  grooveRect.width, grooveRect.height);
                    }
                }

                // restore clip
                g.setClip(saveClip);   

            } else {
                border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                  grooveRect.width, grooveRect.height);
            }
        }

        g.setColor(getTitleColor());
        if(title.length() > 0)
            g.drawString(getTitle(), textLoc.x, textLoc.y);
        icon.paintIcon(c, g, iconLoc.x, iconLoc.y-5);
        g.setFont(font);
        g.setColor(color);
    }
    
    private static boolean computeIntersection(Rectangle dest, 
                                               int rx, int ry, int rw, int rh) {
    	int x1 = Math.max(rx, dest.x);
    	int x2 = Math.min(rx + rw, dest.x + dest.width);
    	int y1 = Math.max(ry, dest.y);
    	int y2 = Math.min(ry + rh, dest.y + dest.height);
        dest.x = x1;
        dest.y = y1;
        dest.width = x2 - x1;
        dest.height = y2 - y1;

        return !(dest.width <= 0 || dest.height <= 0);
    }      
}    
    
        
