package com.limegroup.gnutella.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * A new class to allow mulitple lines to be displayed in a tool tip
 * @author Sumeet Thadani
 */


public class MultilineToolTipUI extends BasicToolTipUI {

    /**
     * The default width of a tooltip.
     */
    private static final int MAX_WIDTH = 400;
    
    /**
     * The blank space between a border & text of a tooltip.
     */
    private static final int BLANK_SPACE = 4;

    /**
     * The shared instance.
     */
    private static final MultilineToolTipUI instance =
        new MultilineToolTipUI();
        
    /**
     * Whether or not the current tip extends beyond the default width
     */
    private boolean extendedWidth = false;
    
    /**
     * The JTextArea with the texts.
     */
    private final JTextArea TEXT = new JTextArea();
    
    /**
     * The cell renderer pane used to draw the text over the component.
     */
    private final CellRendererPane PANE = new CellRendererPane();
    
    public static MultilineToolTipUI instance(){
        return instance;
    }
    
    //private constructor
    private MultilineToolTipUI() {
        TEXT.setLineWrap(true);
        TEXT.setWrapStyleWord(true);
        TEXT.setEditable(false);
        TEXT.setOpaque(false);
    }

    /**
     * Determines the size of this tooltip.
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        extendedWidth = false;
        Font font = c.getFont();
        FontMetrics fontMetrics = c.getFontMetrics(font);
        int fontHeight = fontMetrics.getHeight();
        String[] lines = ((JMultilineToolTip)c).getTipArray();
        if(lines == null)
            return new Dimension(0,0);
        int num_lines = lines.length;
        int height = num_lines * fontHeight;
        if (height==0)
            return new Dimension(0,0);
        int width = 0;
        for (int i = 0; i < num_lines; i++) {
            int line_width = fontMetrics.stringWidth(lines[i]);
            width = Math.max(line_width, width);
            if(width > MAX_WIDTH) {
                extendedWidth = true;
                break;
            }
        }
        
        if(extendedWidth) {
            TEXT.setFont(font);
            TEXT.setText(lines[0]);
            for(int i = 1; i < num_lines; i++)
                TEXT.append("\n" + lines[i]);
            Dimension pref = preferredSize(fontMetrics, TEXT);
            width = pref.width;
            height = pref.height;
        }
        
        return new Dimension(width + BLANK_SPACE * 2,
                             height + BLANK_SPACE * 2);
    }
    
    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }
    
    @Override
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
    
    /**
     * Gets the preferred size of a JTextArea.
     */
    private Dimension preferredSize(final FontMetrics fm, JTextArea area) {
        // Iterate through the text, determining how large we want it to be.
        int fullWidth = 0;
        int rows = 1;
        int carriage = -1;
        int nextCarriage = -1;
        int width = 0;
        final String text = area.getText();
        String current;
        nextCarriage = text.indexOf("\n");
        while(nextCarriage != -1) {
            current = text.substring(carriage+1, nextCarriage);
            width = fm.stringWidth(current);
            if(width > MAX_WIDTH) {
              rows += (int)Math.ceil((double)width / (double)MAX_WIDTH);
              fullWidth = MAX_WIDTH;
            } else {
                fullWidth = Math.max(width, fullWidth);
                rows++;
            }
            carriage = nextCarriage;
            nextCarriage = text.indexOf("\n", carriage+1);
        }
        // last run...
        current = text.substring(carriage + 1);
        width = fm.stringWidth(current);
        if(width > MAX_WIDTH) {
          rows += (int)Math.ceil((double)width / (double)MAX_WIDTH);
          fullWidth = MAX_WIDTH;
        } else {
            fullWidth = Math.max(width, fullWidth);
            rows++;
        }
        return new Dimension(fullWidth, rows * fm.getHeight());
    }
    
    /**
     * Paints this tooltip.  If 'extendedWidth' is true, uses a JTextArea
     * in order to paint the tooltip.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void paint(Graphics g, JComponent c) {
        Dimension dimension = c.getSize();
        Font font = c.getFont();
        
        if(extendedWidth) {
            g.setColor(c.getBackground());
            g.fillRect(0, 0, dimension.width, dimension.height);            
            TEXT.setForeground(c.getForeground());
    		PANE.paintComponent(g, TEXT, c,
    		    BLANK_SPACE, BLANK_SPACE,
    		    dimension.width - BLANK_SPACE*2,
    		    dimension.height - BLANK_SPACE*2,
    		    true);
            return;
        }

        FontMetrics fontMetrics =
        Toolkit.getDefaultToolkit().getFontMetrics(font);
        int fontHeight = fontMetrics.getHeight();
        int fontAscent = fontMetrics.getAscent();
        String[] lines = ((JMultilineToolTip)c).getTipArray();
        // possible NPE
        if (lines == null) return;
        int num_lines = lines.length;
        int height;
        int i;
        
        g.setColor(c.getBackground());
        g.fillRect(0, 0, dimension.width, dimension.height);
        g.setColor(c.getForeground());
        for(i=0, height = BLANK_SPACE + fontAscent;
          i < num_lines; i++, height += fontHeight)
            g.drawString(lines[i], BLANK_SPACE, height);
    }
}
    
